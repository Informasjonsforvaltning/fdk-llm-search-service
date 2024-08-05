package no.digdir.fdk.search.llm.repository

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
open class SearchQueryRepository(
    val jdbcTemplate: NamedParameterJdbcTemplate
) {
    /**
     * Save search query to database
     */
    open fun saveSearchQuery(query: String, nrOfHitsEmbedding: Int, nrOfHitsLlm: Int, sensitive: Boolean) {
        jdbcTemplate.update(
            "INSERT INTO search_queries (query, hits_embedding, hits_llm, sensitive) VALUES (:query, :hits_embedding, :hits_llm, :sensitive)",
            mapOf("query" to query, "hits_embedding" to nrOfHitsEmbedding, "hits_llm" to nrOfHitsLlm, "sensitive" to sensitive)
        )
    }
}
