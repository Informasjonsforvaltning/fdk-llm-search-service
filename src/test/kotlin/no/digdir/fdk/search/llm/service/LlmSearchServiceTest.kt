package no.digdir.fdk.search.llm.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.digdir.fdk.search.llm.model.LlmSearchOperation
import no.digdir.fdk.search.llm.model.SearchType
import no.digdir.fdk.search.llm.model.TextEmbedding
import no.digdir.fdk.search.llm.repository.SearchQueryRepository
import org.junit.Assert.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertEquals

@ActiveProfiles("test")
class LlmSearchServiceTest {
    private val vertexService = mockk<VertexService>()
    private val embeddingService = mockk<EmbeddingService>()
    private val searchQueryRepository = mockk<SearchQueryRepository>()

    private val llmSearchService = LlmSearchService(vertexService, embeddingService, searchQueryRepository)

    @Test
    fun `llm search should return three hits`() {
        val aiResponse = """
             **Kjøretøystatistikk [12345]**

            Dette datasettet inneholder informasjon om førsregistrering, eierskifter, kjøretøybestand og annen informasjon fra Kjøretøyregisteret om utvikling for kjøretøybestanden i Norge. Datasettet oppdateres årlig og inneholder data fra januar 2019. Det er derfor sannsynlig at datasettet inneholder informasjon om Tesla-biler.
            ---
            **Teknisk kjøretøyinformasjon [12346]**
            
            Dette datasettet inneholder teknisk informasjon om kjøretøy, inkludert elbiler. Datasettet oppdateres med ukjent frekvens, men inneholder data fra april 2019. Det er derfor sannsynlig at datasettet inneholder informasjon om Tesla-biler.
            ---
            **Kjøretøyopplysninger [12347]**
            
            Dette datasettet inneholder informasjon om alle registreringspliktige kjøretøy i Norge og dets eiere. Datasettet oppdateres med ukjent frekvens, men inneholder data fra mars 2017. Det er derfor sannsynlig at datasettet inneholder informasjon om Tesla-biler.
        """.trimIndent()

        every { searchQueryRepository.saveSearchQuery("Tesla", any(), any(), false) } returns Unit
        every { vertexService.chat(any()) } returns aiResponse
        every { embeddingService.similaritySearch("Tesla", SearchType.DATASET, 0.3f, 7 ) } returns listOf(
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
    fun `llm search should return no hits`() {
        val aiResponse = """
            **Ingen av datasettene inneholder data som kan brukes til å svare på spørsmålet.**
        """.trimIndent()

        every { searchQueryRepository.saveSearchQuery("Noe som ikke finnes", any(), any(), false) } returns Unit
        every { vertexService.chat(any()) } returns aiResponse
        every { embeddingService.similaritySearch("Noe som ikke finnes", SearchType.DATASET, 0.3f, 7 ) } returns emptyList()

        val result = llmSearchService.search(LlmSearchOperation("Noe som ikke finnes"))
        assertEquals(0, result.hits.size)

        verify {
            searchQueryRepository.saveSearchQuery("Noe som ikke finnes", 0, 0, false)
        }
    }

    @Test
    fun `llm search should return no hits when llm filters summaries`() {
        val aiResponse = """
            **Ingen av datasettene inneholder data som kan brukes til å svare på spørsmålet.**
        """.trimIndent()

        every { searchQueryRepository.saveSearchQuery("Noe som ikke finnes", any(), any(), false) } returns Unit
        every { vertexService.chat(any()) } returns aiResponse
        every { embeddingService.similaritySearch("Noe som ikke finnes", SearchType.DATASET, 0.3f, 7 ) } returns listOf(
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
}
