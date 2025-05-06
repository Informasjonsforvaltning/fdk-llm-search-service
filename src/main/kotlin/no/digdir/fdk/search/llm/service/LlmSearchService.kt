package no.digdir.fdk.search.llm.service


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

        // Perform similarity search
        val embeddings = embeddingService.similaritySearch(
            query, SearchType.DATASET, SIM_THRESHOLD, NUM_MATCHES)

        // Generate AI response using LLM
        val response = vertexService.chat(PromptTemplate.from(PROMPT_TEMPLATE).apply(
            mapOf(
                "summaries" to embeddings.map { it.content },
                "user_query" to query
            )
        ).text())

        logger.debug("AI Response: {}", response)
        val sensitive = response.trim().startsWith("Spørsmålet inneholder muligens personopplysninger")
        val result = parseAiResponse(response, embeddings)

        // Save search query
        searchQueryRepository.saveSearchQuery(query, embeddings.size, result.hits.size, sensitive)

        return result
    }

    /**
     * Parse AI response and extract search hits
     */
    private fun parseAiResponse(response: String, textEmbeddings: List<TextEmbedding>): LlmSearchResult {
        val hits = mutableListOf<LlmSearchHit>()

        val datasets = response.split("---")
        datasets.forEach { dataset ->
            val lines = dataset.trim().split("\n")
            val id = ID_REGEX.find(lines[0])?.groupValues?.get(1) ?: ""
            if(id.isNotEmpty()) {
                textEmbeddings.find { it.id == id }?.let { embedding ->
                    val title = embedding.metadata?.get("title") ?: lines[0].replace(ID_REGEX, "")
                    val description = lines.subList(1, lines.size).joinToString("\n").trim()
                    hits.add(LlmSearchHit(
                        id = id,
                        title = title,
                        description = description,
                        type = embedding.metadata?.get("type") ?: "",
                        publisher = embedding.metadata?.get("publisher") ?: "",
                        publisherId = embedding.metadata?.get("publisherId") ?: "",
                    ))
                }
            }
        }

        return LlmSearchResult(hits)
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(LlmSearchService::class.java)

        private const val NUM_MATCHES = 7
        private const val SIM_THRESHOLD = 0.3f

        private val ID_REGEX = "\\[(.*?)]".toRegex()

        private val PROMPT_TEMPLATE = """
            You will be given a detailed summaries of different datasets in norwegian
            enclosed in triple backticks (```) and a question or query enclosed in
            double backticks(``).
            Select all datasets that are relevant to answer the question.
            Prioritize datasets with newer data.
            Using those dataset summaries, answer the following
            question in as much detail as possible. 
            Give your answer in Norwegian.
            You should only use the information in the summaries.
            Your answer should start with explaining if the question contains possible personal sensitive data and 
            include the dataset title and why each dataset match the question posed by the user.
            If no datasets are given, explain that the data may not exist.
            Give the answer in Markdown and mark the dataset title as bold text and place the id within brackets behind the title.
            Add '---' before each title on a separate line.
                        
                                
            Summaries:
            ```{{summaries}}```
                    
                    
            Question:
            ``{{user_query}}``
                    
                    
            Answer:
            
            """.trimIndent()
    }
}
