package no.digdir.fdk.search.llm.service

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.digdir.fdk.search.llm.configuration.AiProperties
import no.digdir.fdk.search.llm.configuration.SearchProperties
import no.digdir.fdk.search.llm.model.AIResult
import no.digdir.fdk.search.llm.model.AIResultHit
import no.digdir.fdk.search.llm.model.LlmSearchOperation
import no.digdir.fdk.search.llm.model.SearchType
import no.digdir.fdk.search.llm.model.TextEmbedding
import no.digdir.fdk.search.llm.repository.SearchQueryRepository
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertEquals

@ActiveProfiles("test")
class LlmSearchServiceTest {
    private val searchAssistant = mockk<SearchAssistant>()
    private val embeddingService = mockk<EmbeddingService>()
    private val searchQueryRepository = mockk<SearchQueryRepository>()
    private val aiProperties = AiProperties(search = SearchProperties(numMatches = 10, simThreshold = 0.3f))
    private val meterRegistry = SimpleMeterRegistry()

    private val llmSearchService = LlmSearchService(searchAssistant, embeddingService, searchQueryRepository, aiProperties, meterRegistry)

    @Test
    fun `llm search should return three hits`() {
        val aiResult = AIResult(
            sensitive = false,
            hits = listOf(
                AIResultHit("12345", "Kjøretøystatistikk", "Inneholder informasjon om Tesla-biler."),
                AIResultHit("12346", "Teknisk kjøretøyinformasjon", "Inneholder teknisk informasjon om elbiler."),
                AIResultHit("12347", "Kjøretøyopplysninger", "Inneholder informasjon om registrerte kjøretøy.")
            )
        )

        every { searchQueryRepository.saveSearchQuery("Tesla", any(), any(), false) } returns Unit
        every { searchAssistant.answer(any(), "Tesla") } returns aiResult
        every { embeddingService.similaritySearch("Tesla", SearchType.DATASET, 0.3f, 10 ) } returns listOf(
            TextEmbedding("12345", "content", false, 1612137600000,mapOf(
                "title" to "Kjøretøystatistikk",
                "publisher" to "Statistisk sentralbyrå",
                "publisherId" to "1234",
                "type" to SearchType.DATASET.name)),
            TextEmbedding("12346", "content", false, 1612137600000,mapOf(
                "title" to "Teknisk kjøretøyinformasjon",
                "publisher" to "Statistisk sentralbyrå",
                "publisherId" to "1234",
                "type" to SearchType.DATASET.name)),
            TextEmbedding("12347", "content", false, 1612137600000,mapOf(
                "title" to "Kjøretøyopplysninger",
                "publisher" to "Statistisk sentralbyrå",
                "publisherId" to "1234",
                "type" to SearchType.DATASET.name))
        )

        val result = llmSearchService.search(LlmSearchOperation("Tesla"))
        assertEquals(3, result.hits.size)

        assertEquals("Kjøretøystatistikk", result.hits[0].title)
        assertEquals("Teknisk kjøretøyinformasjon", result.hits[1].title)
        assertEquals("Kjøretøyopplysninger", result.hits[2].title)

        assertEquals("Statistisk sentralbyrå", result.hits[0].publisher)
        assertEquals("1234", result.hits[0].publisherId)

        verify {
            searchQueryRepository.saveSearchQuery("Tesla", 3, 3, false)
        }
    }

    @Test
    fun `llm search should return no hits when no embeddings match`() {
        every { searchQueryRepository.saveSearchQuery("Noe som ikke finnes", any(), any(), false) } returns Unit
        every { searchAssistant.answer(any(), "Noe som ikke finnes") } returns AIResult(false, emptyList())
        every { embeddingService.similaritySearch("Noe som ikke finnes", SearchType.DATASET, 0.3f, 10 ) } returns emptyList()

        val result = llmSearchService.search(LlmSearchOperation("Noe som ikke finnes"))
        assertEquals(0, result.hits.size)

        verify {
            searchQueryRepository.saveSearchQuery("Noe som ikke finnes", 0, 0, false)
        }
    }

    @Test
    fun `llm search should return no hits when llm filters summaries`() {
        every { searchQueryRepository.saveSearchQuery("Noe som ikke finnes", any(), any(), false) } returns Unit
        every { searchAssistant.answer(any(), "Noe som ikke finnes") } returns AIResult(false, emptyList())
        every { embeddingService.similaritySearch("Noe som ikke finnes", SearchType.DATASET, 0.3f, 10 ) } returns listOf(
            TextEmbedding("12345", "content", false, 1612137600000, mapOf(
                "title" to "Kjøretøystatistikk",
                "publisher" to "Statistisk sentralbyrå",
                "publisherId" to "1234",
                "type" to SearchType.DATASET.name)),
            TextEmbedding("12346", "content", false, 1612137600000,mapOf(
                "title" to "Teknisk kjøretøyinformasjon",
                "publisher" to "Statistisk sentralbyrå",
                "publisherId" to "1234",
                "type" to SearchType.DATASET.name)),
            TextEmbedding("12347", "content", false, 1612137600000,mapOf(
                "title" to "Kjøretøyopplysninger",
                "publisher" to "Statistisk sentralbyrå",
                "publisherId" to "1234",
                "type" to SearchType.DATASET.name))
        )

        val result = llmSearchService.search(LlmSearchOperation("Noe som ikke finnes"))
        assertEquals(0, result.hits.size)

        verify {
            searchQueryRepository.saveSearchQuery("Noe som ikke finnes", 3, 0, false)
        }
    }

    @Test
    fun `llm search should return no hits when assistant throws`() {
        every { searchQueryRepository.saveSearchQuery("Tesla", any(), any(), false) } returns Unit
        every { searchAssistant.answer(any(), "Tesla") } throws RuntimeException("malformed response")
        every { embeddingService.similaritySearch("Tesla", SearchType.DATASET, 0.3f, 10 ) } returns listOf(
            TextEmbedding("12345", "content", false, 1612137600000, mapOf(
                "title" to "Kjøretøystatistikk",
                "publisher" to "Statistisk sentralbyrå",
                "publisherId" to "1234",
                "type" to SearchType.DATASET.name))
        )

        val result = llmSearchService.search(LlmSearchOperation("Tesla"))
        assertEquals(0, result.hits.size)

        verify {
            searchQueryRepository.saveSearchQuery("Tesla", 1, 0, false)
        }
    }

    @Test
    fun `llm search query must have a minimal length of 3 characters`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            llmSearchService.search(LlmSearchOperation("ab"))
        }
        assertEquals("Query must be at least 3 characters long", exception.message)
    }

    @Test
    fun `llm search query cannot be longer than 255 characters`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            llmSearchService.search(LlmSearchOperation("a".repeat(256)))
        }
        assertEquals("Query cannot be longer than 255 characters", exception.message)
    }

    @Test
    fun `llm search with concept type should filter by concept`() {
        val aiResult = AIResult(
            sensitive = false,
            hits = listOf(AIResultHit("concept-123", "Test Concept", "Relevant concept"))
        )

        every { searchQueryRepository.saveSearchQuery("concept search", any(), any(), false) } returns Unit
        every { searchAssistant.answer(any(), "concept search") } returns aiResult
        every { embeddingService.similaritySearch("concept search", SearchType.CONCEPT, 0.3f, 10) } returns listOf(
            TextEmbedding("concept-123", "content", false, 1612137600000, mapOf(
                "title" to "Test Concept",
                "publisher" to "Test Publisher",
                "publisherId" to "pub-123",
                "type" to SearchType.CONCEPT.name))
        )

        val result = llmSearchService.search(LlmSearchOperation("concept search", SearchType.CONCEPT))
        assertEquals(1, result.hits.size)
        assertEquals("Test Concept", result.hits[0].title)
        assertEquals(SearchType.CONCEPT.name, result.hits[0].type)

        verify {
            embeddingService.similaritySearch("concept search", SearchType.CONCEPT, 0.3f, 10)
        }
    }

    @Test
    fun `llm search with data service type should filter by data service`() {
        val aiResult = AIResult(
            sensitive = false,
            hits = listOf(AIResultHit("dataservice-123", "Test DataService", "Relevant data service"))
        )

        every { searchQueryRepository.saveSearchQuery("dataservice search", any(), any(), false) } returns Unit
        every { searchAssistant.answer(any(), "dataservice search") } returns aiResult
        every { embeddingService.similaritySearch("dataservice search", SearchType.DATA_SERVICE, 0.3f, 10) } returns listOf(
            TextEmbedding("dataservice-123", "content", false, 1612137600000, mapOf(
                "title" to "Test DataService",
                "publisher" to "Test Publisher",
                "publisherId" to "pub-123",
                "type" to SearchType.DATA_SERVICE.name))
        )

        val result = llmSearchService.search(LlmSearchOperation("dataservice search", SearchType.DATA_SERVICE))
        assertEquals(1, result.hits.size)
        assertEquals("Test DataService", result.hits[0].title)
        assertEquals(SearchType.DATA_SERVICE.name, result.hits[0].type)

        verify {
            embeddingService.similaritySearch("dataservice search", SearchType.DATA_SERVICE, 0.3f, 10)
        }
    }

    @Test
    fun `llm search defaults to dataset type when not specified`() {
        val aiResult = AIResult(
            sensitive = false,
            hits = listOf(AIResultHit("dataset-123", "Test Dataset", "Relevant dataset"))
        )

        every { searchQueryRepository.saveSearchQuery("default search", any(), any(), false) } returns Unit
        every { searchAssistant.answer(any(), "default search") } returns aiResult
        every { embeddingService.similaritySearch("default search", SearchType.DATASET, 0.3f, 10) } returns listOf(
            TextEmbedding("dataset-123", "content", false, 1612137600000, mapOf(
                "title" to "Test Dataset",
                "publisher" to "Test Publisher",
                "publisherId" to "pub-123",
                "type" to SearchType.DATASET.name))
        )

        val result = llmSearchService.search(LlmSearchOperation("default search"))
        assertEquals(1, result.hits.size)
        assertEquals("Test Dataset", result.hits[0].title)
        assertEquals(SearchType.DATASET.name, result.hits[0].type)

        verify {
            embeddingService.similaritySearch("default search", SearchType.DATASET, 0.3f, 10)
        }
    }

    @Test
    fun `records metrics counter with expected tags on normal search`() {
        val aiResult = AIResult(
            sensitive = false,
            hits = listOf(AIResultHit("12345", "Kjøretøystatistikk", "Inneholder informasjon om Tesla-biler."))
        )

        every { searchQueryRepository.saveSearchQuery("Tesla metric", any(), any(), false) } returns Unit
        every { searchAssistant.answer(any(), "Tesla metric") } returns aiResult
        every { embeddingService.similaritySearch("Tesla metric", SearchType.DATASET, 0.3f, 10) } returns listOf(
            TextEmbedding("12345", "content", false, 1612137600000, mapOf(
                "title" to "Kjøretøystatistikk",
                "publisher" to "Statistisk sentralbyrå",
                "publisherId" to "1234",
                "type" to SearchType.DATASET.name))
        )

        llmSearchService.search(LlmSearchOperation("Tesla metric"))

        val counter = meterRegistry.find("fdk_llm_search_queries_total")
            .tag("type", SearchType.DATASET.name)
            .tag("zero_hits", "false")
            .tag("llm_failed", "false")
            .tag("sensitive", "false")
            .counter()
        assertEquals(1.0, counter?.count())

        val embeddingSummary = meterRegistry.find("fdk_llm_search_hits")
            .tag("stage", "embedding")
            .summary()
        assertEquals(1L, embeddingSummary?.count())

        val llmSummary = meterRegistry.find("fdk_llm_search_hits")
            .tag("stage", "llm")
            .summary()
        assertEquals(1L, llmSummary?.count())
    }

    @Test
    fun `records llm_failed=true when assistant throws`() {
        every { searchQueryRepository.saveSearchQuery("Tesla fail", any(), any(), false) } returns Unit
        every { searchAssistant.answer(any(), "Tesla fail") } throws RuntimeException("malformed response")
        every { embeddingService.similaritySearch("Tesla fail", SearchType.DATASET, 0.3f, 10) } returns listOf(
            TextEmbedding("12345", "content", false, 1612137600000, mapOf(
                "title" to "Kjøretøystatistikk",
                "publisher" to "Statistisk sentralbyrå",
                "publisherId" to "1234",
                "type" to SearchType.DATASET.name))
        )

        llmSearchService.search(LlmSearchOperation("Tesla fail"))

        val counter = meterRegistry.find("fdk_llm_search_queries_total")
            .tag("type", SearchType.DATASET.name)
            .tag("zero_hits", "true")
            .tag("llm_failed", "true")
            .counter()
        assertEquals(1.0, counter?.count())
    }

    @Test
    fun `records zero_hits=true when llm returns no hits`() {
        every { searchQueryRepository.saveSearchQuery("empty query", any(), any(), false) } returns Unit
        every { searchAssistant.answer(any(), "empty query") } returns AIResult(false, emptyList())
        every { embeddingService.similaritySearch("empty query", SearchType.DATASET, 0.3f, 10) } returns emptyList()

        llmSearchService.search(LlmSearchOperation("empty query"))

        val counter = meterRegistry.find("fdk_llm_search_queries_total")
            .tag("zero_hits", "true")
            .tag("llm_failed", "false")
            .counter()
        assertEquals(1.0, counter?.count())
    }

    @Test
    fun `records sensitive=true tag when result is flagged sensitive`() {
        every { searchQueryRepository.saveSearchQuery("Personnummer 12345678901", any(), any(), true) } returns Unit
        every { searchAssistant.answer(any(), "Personnummer 12345678901") } returns AIResult(sensitive = true, hits = emptyList())
        every { embeddingService.similaritySearch("Personnummer 12345678901", SearchType.DATASET, 0.3f, 10) } returns emptyList()

        llmSearchService.search(LlmSearchOperation("Personnummer 12345678901"))

        val counter = meterRegistry.find("fdk_llm_search_queries_total")
            .tag("sensitive", "true")
            .counter()
        assertEquals(1.0, counter?.count())
    }

    @Test
    fun `llm search with ALL type should search across all resource types`() {
        val aiResult = AIResult(
            sensitive = false,
            hits = listOf(AIResultHit("mixed-123", "Mixed Resource", "Relevant resource"))
        )

        every { searchQueryRepository.saveSearchQuery("all types search", any(), any(), false) } returns Unit
        every { searchAssistant.answer(any(), "all types search") } returns aiResult
        every { embeddingService.similaritySearch("all types search", null, 0.3f, 10) } returns listOf(
            TextEmbedding("mixed-123", "content", false, 1612137600000, mapOf(
                "title" to "Mixed Resource",
                "publisher" to "Test Publisher",
                "publisherId" to "pub-123",
                "type" to SearchType.DATASET.name))
        )

        val result = llmSearchService.search(LlmSearchOperation("all types search", SearchType.ALL))
        assertEquals(1, result.hits.size)
        assertEquals("Mixed Resource", result.hits[0].title)

        verify {
            embeddingService.similaritySearch("all types search", null, 0.3f, 10)
        }
    }
}
