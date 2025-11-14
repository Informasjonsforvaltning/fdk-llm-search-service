package no.digdir.fdk.search.llm.service


import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.langchain4j.model.input.PromptTemplate
import no.digdir.fdk.search.llm.model.*
import no.digdir.fdk.search.llm.repository.SearchQueryRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component


@Component
class LlmSearchService(
    private val vertexService: VertexService,
    private val embeddingService: EmbeddingService,
    private val searchQueryRepository: SearchQueryRepository
) {
    private val minQueryLength = 3
    private val maxQueryLength = 255

    private fun validateQuery(query: String) {
        if (query.length < minQueryLength) {
            throw IllegalArgumentException("Query must be at least $minQueryLength characters long")
        }
        if (query.length > maxQueryLength) {
            throw IllegalArgumentException("Query cannot be longer than $maxQueryLength characters")
        }
    }

    /**
     * Perform similarity search and generate AI response
     */
    fun search(searchOperation: LlmSearchOperation): LlmSearchResult {
        validateQuery(searchOperation.query)

        logger.debug("Search operation: {}", searchOperation)

        // Escape special characters
        val query = searchOperation.query.replace("`", "'")

        // Perform similarity search, filtered by resource type (defaults to DATASET, use ALL for all types)
        val searchType = if (searchOperation.type == SearchType.ALL) null else searchOperation.type
        val embeddings = embeddingService.similaritySearch(
            query, searchType, SIM_THRESHOLD, NUM_MATCHES)

        val message = PromptTemplate.from(PROMPT_TEMPLATE).apply(
            mapOf(
                "summaries" to objectMapper.writeValueAsString(embeddings),
                "user_query" to query
            )
        ).text()

        if(logger.isDebugEnabled) {
            logger.debug("Chat message: {}", message)
        }

        // Generate AI response using LLM
        val response = vertexService.chat(message)

        logger.debug("AI Response: {}", response)
        val result = parseAiResponse(response)

        // Save search query
        searchQueryRepository.saveSearchQuery(query, embeddings.size, result.hits.size, result.sensitive)

        return LlmSearchResult(
            hits = result.hits.map { hit ->
                val embedding = embeddings.find { it.id == hit.id }
                LlmSearchHit(
                    id = hit.id,
                    title = hit.name,
                    description = hit.reason,
                    type = embedding?.metadata?.get("type") ?: "",
                    publisher = embedding?.metadata?.get("publisher") ?: "",
                    publisherId = embedding?.metadata?.get("publisherId") ?: "",
                )
            }
        )
    }

    /**
     * Parse AI response and extract search hits
     */
    private fun parseAiResponse(response: String): AIResult {
        val jsonString = Regex("```json\\s+(.*?)\\s+```", RegexOption.DOT_MATCHES_ALL)
            .find(response)
            ?.groupValues
            ?.get(1)

        return jsonString?.let {
            objectMapper.readValue(it, AIResult::class.java)
        } ?: AIResult(false, emptyList())
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(LlmSearchService::class.java)

        private val objectMapper: ObjectMapper = jacksonObjectMapper()

        private const val NUM_MATCHES = 10
        private const val SIM_THRESHOLD = 0.3f

        private val PROMPT_TEMPLATE = """
            You will be given a detailed summaries of different resources (datasets, concepts, data services, information models, services, and events) in norwegian as a JSON array.
            The question is enclosed in double backticks(``).
            Select all resources that are relevant to answer the question.
            Prioritize resources with newer data when applicable.
            Using those resource summaries, answer the question in as much detail as possible. 
            Give your answer in Norwegian.
            You should only use the information in the summaries.
            Your answer should start with explaining if the question contains possible personal sensitive data 
            (sensitive) and why each resource matches the question posed by the user (reason).
            Format the result as JSON only using the following structure format the description in Markdown: 
            ```json
            { "sensitive": true/false, "hits": [ { "id": "", "name": "", "reason": "" } ] }
            ```
                                            
            Summaries:
            ```json
            {{summaries}}
            ```        
                    
            Question:
            ``{{user_query}}``            
            """.trimIndent()
    }
}
