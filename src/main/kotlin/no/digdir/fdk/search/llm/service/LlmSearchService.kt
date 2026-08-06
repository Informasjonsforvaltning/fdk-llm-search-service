package no.digdir.fdk.search.llm.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.Timer
import no.digdir.fdk.search.llm.configuration.AiProperties
import no.digdir.fdk.search.llm.configuration.SearchProperties
import no.digdir.fdk.search.llm.model.AIResult
import no.digdir.fdk.search.llm.model.LlmSearchHit
import no.digdir.fdk.search.llm.model.LlmSearchOperation
import no.digdir.fdk.search.llm.model.LlmSearchResult
import no.digdir.fdk.search.llm.model.SearchType
import no.digdir.fdk.search.llm.model.TextEmbedding
import no.digdir.fdk.search.llm.repository.SearchQueryRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.TimeUnit

@Component
class LlmSearchService(
    private val searchAssistant: SearchAssistant,
    private val embeddingService: EmbeddingService,
    private val searchQueryRepository: SearchQueryRepository,
    private val aiProperties: AiProperties,
    private val meterRegistry: MeterRegistry,
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

        val query = searchOperation.query

        // Perform similarity search, filtered by resource type (defaults to DATASET, use ALL for all types)
        val searchType = if (searchOperation.type == SearchType.ALL) null else searchOperation.type
        val search = aiProperties.search ?: SearchProperties()

        val embeddingStart = System.nanoTime()
        val embeddings =
            embeddingService.similaritySearch(query, searchType, search.simThreshold, search.numMatches)
        val embeddingNanos = System.nanoTime() - embeddingStart

        val llmStart = System.nanoTime()
        val (result, llmFailed) =
            runCatching {
                searchAssistant.answer(objectMapper.writeValueAsString(embeddings), query)
            }.fold(
                onSuccess = { aiResult -> aiResult to false },
                onFailure = { ex ->
                    logger.warn("Failed to obtain structured response from LLM", ex)
                    AIResult(false, emptyList()) to true
                },
            )
        val llmNanos = System.nanoTime() - llmStart

        logger.debug("AI Result: {}", result)

        searchQueryRepository.saveSearchQuery(query, embeddings.size, result.hits.size, result.sensitive)

        recordTelemetry(
            query = query,
            searchType = searchOperation.type ?: SearchType.DATASET,
            embeddings = embeddings,
            hitsLlm = result.hits.size,
            sensitive = result.sensitive,
            llmFailed = llmFailed,
            embeddingNanos = embeddingNanos,
            llmNanos = llmNanos,
        )

        return LlmSearchResult(
            hits =
                result.hits.map { hit ->
                    val embedding = embeddings.find { it.id == hit.id }
                    LlmSearchHit(
                        id = hit.id,
                        title = hit.name,
                        description = hit.reason,
                        type = embedding?.metadata?.get("type") ?: "",
                        publisher = embedding?.metadata?.get("publisher") ?: "",
                        publisherId = embedding?.metadata?.get("publisherId") ?: "",
                    )
                },
        )
    }

    private fun recordTelemetry(
        query: String,
        searchType: SearchType,
        embeddings: List<TextEmbedding>,
        hitsLlm: Int,
        sensitive: Boolean,
        llmFailed: Boolean,
        embeddingNanos: Long,
        llmNanos: Long,
    ) {
        try {
            val hitsEmbedding = embeddings.size
            val zeroHits = hitsLlm == 0

            recordSearchMetrics(
                searchType = searchType,
                hitsEmbedding = hitsEmbedding,
                hitsLlm = hitsLlm,
                zeroHits = zeroHits,
                sensitive = sensitive,
                llmFailed = llmFailed,
                embeddingNanos = embeddingNanos,
                llmNanos = llmNanos,
            )
            logSearchCompleted(
                query = query,
                searchType = searchType,
                embeddings = embeddings,
                hitsEmbedding = hitsEmbedding,
                hitsLlm = hitsLlm,
                zeroHits = zeroHits,
                sensitive = sensitive,
                llmFailed = llmFailed,
                embeddingNanos = embeddingNanos,
                llmNanos = llmNanos,
            )
        } catch (ex: Exception) {
            logger.warn("Failed to record search telemetry", ex)
        }
    }

    private fun recordSearchMetrics(
        searchType: SearchType,
        hitsEmbedding: Int,
        hitsLlm: Int,
        zeroHits: Boolean,
        sensitive: Boolean,
        llmFailed: Boolean,
        embeddingNanos: Long,
        llmNanos: Long,
    ) {
        val tags =
            Tags.of(
                "type",
                searchType.name,
                "zero_hits",
                zeroHits.toString(),
                "llm_failed",
                llmFailed.toString(),
                "sensitive",
                sensitive.toString(),
            )

        meterRegistry.counter("fdk_llm_search_queries_total", tags).increment()
        recordPhaseTimer("embedding", embeddingNanos)
        recordPhaseTimer("llm", llmNanos)
        recordHits("embedding", hitsEmbedding)
        recordHits("llm", hitsLlm)
    }

    private fun logSearchCompleted(
        query: String,
        searchType: SearchType,
        embeddings: List<TextEmbedding>,
        hitsEmbedding: Int,
        hitsLlm: Int,
        zeroHits: Boolean,
        sensitive: Boolean,
        llmFailed: Boolean,
        embeddingNanos: Long,
        llmNanos: Long,
    ) {
        val safeQuery = if (sensitive) "[REDACTED]" else query
        val mdc =
            mutableMapOf(
                "event" to "llm_search",
                "query" to safeQuery,
                "query_length" to query.length.toString(),
                "search_type" to searchType.name,
                "hits_embedding" to hitsEmbedding.toString(),
                "hits_llm" to hitsLlm.toString(),
                "zero_hits" to zeroHits.toString(),
                "sensitive" to sensitive.toString(),
                "llm_failed" to llmFailed.toString(),
                "embedding_ms" to TimeUnit.NANOSECONDS.toMillis(embeddingNanos).toString(),
                "llm_ms" to TimeUnit.NANOSECONDS.toMillis(llmNanos).toString(),
            )
        if (zeroHits && hitsEmbedding > 0 && !sensitive) {
            mdc["embedding_hits"] = serializeEmbeddingHits(embeddings)
        }
        mdc.forEach { (k, v) -> MDC.put(k, v) }
        try {
            logger.info("llm_search completed")
        } finally {
            mdc.keys.forEach { MDC.remove(it) }
        }
    }

    private fun recordPhaseTimer(
        phase: String,
        nanos: Long,
    ) {
        Timer
            .builder("fdk_llm_search_phase_duration")
            .tag("phase", phase)
            .publishPercentileHistogram()
            .serviceLevelObjectives(
                Duration.ofMillis(500),
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                Duration.ofSeconds(5),
                Duration.ofSeconds(10),
            ).register(meterRegistry)
            .record(nanos, TimeUnit.NANOSECONDS)
    }

    private fun recordHits(
        stage: String,
        hits: Int,
    ) {
        DistributionSummary
            .builder("fdk_llm_search_hits")
            .tag("stage", stage)
            .publishPercentileHistogram()
            .serviceLevelObjectives(0.5, 1.0, 3.0, 5.0, 10.0)
            .register(meterRegistry)
            .record(hits.toDouble())
    }

    private fun serializeEmbeddingHits(embeddings: List<TextEmbedding>): String =
        objectMapper.writeValueAsString(
            embeddings.map { e ->
                mapOf(
                    "id" to e.id,
                    "type" to e.metadata?.get("type"),
                    "publisherId" to e.metadata?.get("publisherId"),
                    "content" to e.content,
                )
            },
        )

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(LlmSearchService::class.java)

        private val objectMapper: ObjectMapper = jacksonObjectMapper()
    }
}
