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
        TextEmbedding(
            id = rs.getString("id"),
            content = rs.getString("content"),
            deleted = rs.getBoolean("deleted"),
            timestamp = rs.getLong("timestamp"),
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
     * Check if message should be processed based on timestamp
     * @return true if message should be processed (timestamp is newer or embedding doesn't exist), false otherwise
     */
    fun shouldProcessMessage(id: String, timestamp: Long): Boolean {
        val textEmbedding = findById(id)
        return textEmbedding == null || textEmbedding.timestamp < timestamp
    }

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
    fun searchSimilar(type: SearchType?, vector: FloatArray, simThreshold: Float, numMatches: Int): List<TextEmbedding> {
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

    companion object {
        private val objectMapper: ObjectMapper = jacksonObjectMapper()
        private val metadataMapType =
            TypeFactory
                .defaultInstance()
                .constructMapType(HashMap::class.java, String::class.java, String::class.java)
    }
}
