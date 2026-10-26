package no.digdir.fdk.search.llm.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.pgvector.PGvector
import no.digdir.fdk.search.llm.model.SearchType
import no.digdir.fdk.search.llm.model.TextEmbedding
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.sql.ResultSet

@Component
class EmbeddingRepository(private val jdbcTemplate: NamedParameterJdbcTemplate) {
    private val rowMapper: (ResultSet, rowNum: Int) -> TextEmbedding? = { rs, _ ->
        val timestamp = rs.getLong("timestamp").takeUnless { rs.wasNull() }
        TextEmbedding(
            id = rs.getString("id"),
            content = rs.getString("content"),
            deleted = rs.getBoolean("deleted"),
            timestamp = timestamp,
            metadata =
            objectMapper.readValue(
                rs.getString("metadata") ?: "{}",
                metadataMapType,
            ),
        )
    }

    internal fun findById(id: String): TextEmbedding? = jdbcTemplate
        .query(
            """
                SELECT id, content, timestamp, deleted, metadata FROM embeddings WHERE id = :id
            """.trimIndent(),
            mapOf("id" to id),
            rowMapper,
        ).firstOrNull()

    /**
     * Check if message should be processed based on timestamp.
     * Matches the SQL upsert guard: process when no row exists, stored timestamp is NULL,
     * or [timestamp] is strictly greater than the stored value.
     */
    fun shouldProcessMessage(id: String, timestamp: Long): Boolean {
        val textEmbedding = findById(id) ?: return true
        return isNewerThanStored(timestamp, textEmbedding.timestamp)
    }

    /**
     * Same ordering rule as
     * `embeddings.timestamp IS NULL OR embeddings.timestamp < EXCLUDED.timestamp`.
     */
    internal fun isNewerThanStored(incoming: Long, stored: Long?): Boolean = stored == null || stored < incoming

    /**
     * Save embedding to database.
     * Atomically upserts only when [timestamp] is newer than the stored value, so a concurrent
     * remove with a newer timestamp cannot be overwritten by an older parse event.
     *
     * @return true if the embedding was saved/updated, false if skipped due to older timestamp
     */
    fun saveEmbedding(id: String, content: String, vector: FloatArray, timestamp: Long, metadata: Map<String, String?>): Boolean {
        val params =
            mapOf(
                "id" to id,
                "content" to content,
                "embedding" to PGvector(vector),
                "metadata" to objectMapper.writeValueAsString(metadata),
                "timestamp" to timestamp,
            )

        val updated =
            jdbcTemplate.update(
                """
                INSERT INTO embeddings (id, content, timestamp, deleted, metadata, embedding)
                    VALUES (:id, :content, :timestamp, false, :metadata::jsonb, :embedding)
                ON CONFLICT (id) DO UPDATE SET
                    content = EXCLUDED.content,
                    timestamp = EXCLUDED.timestamp,
                    deleted = false,
                    metadata = EXCLUDED.metadata,
                    embedding = EXCLUDED.embedding
                WHERE embeddings.timestamp IS NULL OR embeddings.timestamp < EXCLUDED.timestamp
                """.trimIndent(),
                params,
            )
        return updated > 0
    }

    /**
     * Mark embedding as deleted by id.
     * Atomically upserts a delete only when [timestamp] is newer than the stored value, so a
     * concurrent save with an older timestamp cannot win the race.
     *
     * @return true if the embedding was marked as deleted, false if skipped due to older timestamp
     */
    fun markDeletedByIdAndBeforeTimestamp(id: String, timestamp: Long): Boolean {
        val updated =
            jdbcTemplate.update(
                """
                INSERT INTO embeddings (id, timestamp, deleted) VALUES (:id, :timestamp, true)
                ON CONFLICT (id) DO UPDATE SET
                    timestamp = EXCLUDED.timestamp,
                    deleted = true
                WHERE embeddings.timestamp IS NULL OR embeddings.timestamp < EXCLUDED.timestamp
                """.trimIndent(),
                mapOf("id" to id, "timestamp" to timestamp),
            )
        return updated > 0
    }

    /**
     * Perform similarity search
     */
    fun searchSimilar(
        type: SearchType?,
        vector: FloatArray,
        simThreshold: Float,
        numMatches: Int,
        lexicalQuery: String? = null,
    ): List<TextEmbedding> {
        val typeFilter = if (type != null) "AND metadata->>'type' = :type" else ""
        val params =
            mutableMapOf<String, Any>(
                "vector" to PGvector(vector),
                "simThreshold" to simThreshold,
                "numMatches" to numMatches,
            )
        if (type != null) {
            params["type"] = type.name
        }
        if (lexicalQuery != null) {
            params["lexicalQuery"] = lexicalQuery
            params["rrfK"] = RRF_K
            params["lexicalMatches"] = LEXICAL_MATCHES
            return jdbcTemplate.query(hybridSearchSql(typeFilter), params, rowMapper).requireNoNulls()
        }

        return jdbcTemplate
            .query(
                """
           SELECT id, content, deleted, timestamp, embedding, metadata, 1 - (embedding <=> :vector) AS similarity
           FROM embeddings
           WHERE 1 - (embedding <=> :vector) > :simThreshold $typeFilter AND deleted IS NOT TRUE
           ORDER BY similarity DESC
           LIMIT :numMatches
       """,
                params,
                rowMapper,
            ).requireNoNulls()
    }

    /**
     * Fuses the embedding ranking with a full-text ranking on the title using reciprocal rank fusion,
     * so exact terms such as years ("2026") weigh in where the embedding barely tells them apart.
     * Query terms are OR-ed, so natural extra words in the question do not exclude title matches. Only the
     * best [LEXICAL_MATCHES] title matches count, so titles sharing a single generic word with the query
     * (e.g. "jordbruksforetak") do not outrank closer semantic matches.
     */
    private fun hybridSearchSql(typeFilter: String) = """
        WITH vec AS (
            SELECT id, row_number() OVER (ORDER BY embedding <=> :vector, id) AS rnk
            FROM embeddings
            WHERE 1 - (embedding <=> :vector) > :simThreshold $typeFilter AND deleted IS NOT TRUE
            ORDER BY rnk
            LIMIT :numMatches
        ),
        lex AS (
            SELECT id, row_number() OVER (ORDER BY ts_rank_cd(title_tsv, q.tsq) DESC, id) AS rnk
            FROM embeddings,
                 (SELECT replace(plainto_tsquery('norwegian', :lexicalQuery)::text, ' & ', ' | ')::tsquery AS tsq) q
            WHERE title_tsv @@ q.tsq $typeFilter AND deleted IS NOT TRUE
            ORDER BY rnk
            LIMIT :lexicalMatches
        ),
        fused AS (
            SELECT id, sum(1.0 / (:rrfK + rnk)) AS score
            FROM (SELECT id, rnk FROM vec UNION ALL SELECT id, rnk FROM lex) ranked
            GROUP BY id
        )
        SELECT e.id, e.content, e.deleted, e.timestamp, e.metadata
        FROM fused f JOIN embeddings e ON e.id = f.id
        ORDER BY f.score DESC, e.id
        LIMIT :numMatches
    """.trimIndent()

    companion object {
        private const val RRF_K = 60
        private const val LEXICAL_MATCHES = 10
        private val objectMapper: ObjectMapper = jacksonObjectMapper()
        private val metadataMapType =
            TypeFactory
                .defaultInstance()
                .constructMapType(HashMap::class.java, String::class.java, String::class.java)
    }
}
