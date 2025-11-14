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
                rs.getString("metadata") ?: "{}",
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
     * Check if message should be processed based on timestamp
     * @return true if message should be processed (timestamp is newer or embedding doesn't exist), false otherwise
     */
    fun shouldProcessMessage(id: String, timestamp: Long): Boolean {
        val textEmbedding = findById(id)
        return textEmbedding == null || textEmbedding.timestamp < timestamp
    }

    /**
     * Save embedding to database
     * Only processes if the message timestamp is newer than the existing embedding timestamp.
     * Sets deleted flag to false since we're sure this save happened after any previous delete.
     * 
     * @return true if the embedding was saved/updated, false if skipped due to older timestamp
     */
    fun saveEmbedding(id: String, content: String, vector: FloatArray, timestamp: Long, metadata: Map<String, String?>): Boolean {
        val textEmbedding = findById(id)
        
        // If embedding exists and message timestamp is not newer, skip processing
        if (textEmbedding != null && textEmbedding.timestamp >= timestamp) {
            return false
        }
        
        val params = mapOf(
            "id" to id,
            "content" to content,
            "embedding" to PGvector(vector),
            "metadata" to jacksonObjectMapper().writeValueAsString(metadata),
            "timestamp" to timestamp)

        if (textEmbedding == null) {
            // New embedding - set deleted to false
            jdbcTemplate.update("""
                INSERT INTO embeddings (id, content, timestamp, deleted, metadata, embedding) 
                    VALUES (:id, :content, :timestamp, false, :metadata::jsonb, :embedding)
                """.trimIndent(), params)
        } else {
            // Update existing embedding - message timestamp is newer, so set deleted to false
            // This ensures that if there was a previous delete, this save overrides it
            jdbcTemplate.update("""
                UPDATE embeddings SET content = :content, timestamp = :timestamp, deleted = false, 
                    metadata = :metadata::jsonb, embedding = :embedding WHERE id = :id
            """.trimIndent(), params)
        }
        return true
    }

    /**
     * Mark embedding as deleted by id
     * Only processes if the message timestamp is newer than the existing embedding timestamp.
     * 
     * @return true if the embedding was marked as deleted, false if skipped due to older timestamp
     */
    fun markDeletedByIdAndBeforeTimestamp(id: String, timestamp: Long): Boolean {
        val textEmbedding = findById(id)
        
        // If embedding exists and message timestamp is not newer, skip processing
        if (textEmbedding != null && textEmbedding.timestamp >= timestamp) {
            return false
        }
        
        val params = mapOf("id" to id, "timestamp" to timestamp)
        
        if (textEmbedding == null) {
            // New deletion record - embedding doesn't exist yet, mark as deleted
            jdbcTemplate.update("""
                INSERT INTO embeddings (id, timestamp, deleted) VALUES (:id, :timestamp, true) 
            """.trimIndent(), params)
        } else {
            // Update existing embedding - message timestamp is newer, so mark as deleted
            jdbcTemplate.update("""
                UPDATE embeddings SET timestamp = :timestamp, deleted = true WHERE id = :id
            """, params)
        }
        return true
    }

    /**
     * Perform similarity search
     */
    fun searchSimilar(type: SearchType?, vector: FloatArray, simThreshold: Float, numMatches: Int): List<TextEmbedding> {
        val typeFilter = if (type != null) "AND metadata->>'type' = :type" else ""
        val params = mutableMapOf<String, Any>(
            "vector" to PGvector(vector),
            "simThreshold" to simThreshold,
            "numMatches" to numMatches
        )
        if (type != null) {
            params["type"] = type.name
        }
        
        return jdbcTemplate.query("""
           SELECT id, content, deleted, timestamp, embedding, metadata, 1 - (embedding <=> :vector) AS similarity
           FROM embeddings
           WHERE 1 - (embedding <=> :vector) > :simThreshold $typeFilter AND deleted IS NOT TRUE
           ORDER BY similarity DESC
           LIMIT :numMatches
       """, params, rowMapper).requireNoNulls()
    }
}
