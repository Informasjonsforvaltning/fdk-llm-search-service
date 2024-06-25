package no.digdir.fdk.search.llm.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.pgvector.PGvector
import no.digdir.fdk.search.llm.model.TextEmbedding
import no.digdir.fdk.search.llm.model.SearchType
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.sql.ResultSet

@Component
class EmbeddingRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) {
    private val rowMapper: (ResultSet, rowNum: Int) -> TextEmbedding? = { rs, _ ->
        TextEmbedding(
            id = rs.getString("id"),
            content = rs.getString("content"),
            deleted = rs.getBoolean("deleted"),
            timestamp = rs.getLong("timestamp"),
            metadata = ObjectMapper().readValue(
                rs.getString("metadata"),
                TypeFactory.defaultInstance()
                    .constructMapType(HashMap::class.java, String::class.java, String::class.java)
            ),
        )
    }

    private fun findById(id: String): TextEmbedding? {
        return jdbcTemplate.query("""
            SELECT id, content, timestamp, deleted, metadata FROM embeddings WHERE id = :id
        """.trimIndent(), mapOf("id" to id), rowMapper).firstOrNull()
    }

    /**
     * Save embedding to database
     */
    fun saveEmbedding(id: String, content: String, vector: FloatArray, timestamp: Long, metadata: Map<String, String?>) {
        val params = mapOf(
            "id" to id,
            "content" to content,
            "embedding" to PGvector(vector),
            "metadata" to jacksonObjectMapper().writeValueAsString(metadata),
            "timestamp" to timestamp)

        val textEmbedding = findById(id)
        if (textEmbedding == null) {
            jdbcTemplate.update("""
                INSERT INTO embeddings (id, content, timestamp, deleted, metadata, embedding) 
                    VALUES (:id, :content, :timestamp, false, :metadata::jsonb, :embedding)
                """.trimIndent(), params)
        } else if(textEmbedding.timestamp < timestamp) {
            jdbcTemplate.update("""
                UPDATE embeddings SET content = :content, timestamp = :timestamp, deleted = false, 
                    metadata = :metadata::jsonb, embedding = :embedding WHERE id = :id
            """.trimIndent(), params)
        }
    }

    /**
     * Delete embedding by id
     */
    fun markDeletedByIdAndBeforeTimestamp(id: String, timestamp: Long) {
        val params = mapOf("id" to id, "timestamp" to timestamp)
        val textEmbedding = findById(id)
        if (textEmbedding == null) {
            jdbcTemplate.update("""
                INSERT INTO embeddings (id, timestamp, deleted) VALUES (:id, :timestamp, true) 
            """.trimIndent(), params)
        } else if (textEmbedding.timestamp < timestamp) {
            jdbcTemplate.update("""
                UPDATE embeddings SET timestamp = :timestamp, deleted = true WHERE id = :id
            """, params)
        }
    }

    /**
     * Perform similarity search
     */
    fun searchSimilar(type: SearchType, vector: FloatArray, simThreshold: Float, numMatches: Int): List<TextEmbedding> {
        return jdbcTemplate.query("""
           SELECT id, content, deleted, timestamp, embedding, metadata, 1 - (embedding <=> :vector) AS similarity
           FROM embeddings
           WHERE 1 - (embedding <=> :vector) > :simThreshold AND metadata->>'type' = :type AND deleted IS NOT TRUE
           ORDER BY similarity DESC
           LIMIT :numMatches
       """, mapOf(
           "type" to type.name,
           "vector" to PGvector(vector),
           "simThreshold" to simThreshold,
           "numMatches" to numMatches
       ), rowMapper).requireNoNulls()
    }
}
