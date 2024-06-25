package no.digdir.fdk.search.llm.service

import dev.langchain4j.data.embedding.Embedding
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.digdir.fdk.search.llm.model.*
import no.digdir.fdk.search.llm.repository.EmbeddingRepository
import no.digdir.fdk.search.llm.repository.SearchQueryRepository
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertEquals

@ActiveProfiles("test")
class EmbeddingServiceTest {
    private val vertexService = mockk<VertexService>()
    private val embeddingRepository = mockk<EmbeddingRepository>()

    private val embeddingService = EmbeddingService(vertexService, embeddingRepository)

    @Test
    fun `store dataset embedding`() {
        val vector = FloatArray(768)
        vector.fill(0.1f)

        val expectedSummary = """
Dette datasettet, med id '12345' og navn 'NB Test title' er utgitt av 'NB Test publisher > prefLabel'.
Datasettet har public tilgang.

Beskrivelsen av datasettet er som følger:
description
 
Datasettet ble utgitt 01.01.2021 og oppdateres NB Test accrualPeriodicity > prefLabel.
Datasettet har 1 distribusjoner og tilbyr data på formatene json.
Temaene for datasettet er: eutheme1, eutheme2, lostheme1.
Nøkkelordene for datasettet er: keyword.
Dataen er tidsmessig begrenset: 2021-01-01 til 2021-12-31.
        """.trimIndent()

        every { vertexService.embed(any()) } returns Embedding(vector)
        every { embeddingRepository.saveEmbedding(any(), any(), any(), any(), any()) } returns Unit

        embeddingService.storeDatasetEmbedding("12345", TEST_DATASET_ALL_FIELDS, 1612137600000L)

        verify {
            embeddingRepository.saveEmbedding(
                "12345", expectedSummary, vector, 1612137600000, mapOf(
                    "type" to SearchType.DATASET.name,
                    "title" to "NB Test title",
                    "publisher" to "NB Test publisher > prefLabel",
                    "publisherId" to "Test publisher > identifier"
                )
            )
        }
    }

    @Test
    fun `similarity search should return two embeddings`() {
        val vector = FloatArray(768)
        vector.fill(0.5f)

        val expectedEmbeddings = listOf(
            TextEmbedding("12345", "content1", false, 1612137600000, mapOf("title" to "title1")),
            TextEmbedding("12346", "content2", false, 1612137600000, mapOf("title" to "title2"))
        )

        every { vertexService.embed(any()) } returns Embedding(vector)
        every { embeddingRepository.searchSimilar(any(), any(), any(), any()) } returns expectedEmbeddings
        val embeddings = embeddingService.similaritySearch("search_query", SearchType.DATASET, 0.3f, 7)

        assertEquals(2, embeddings.size)
        assertEquals(expectedEmbeddings, embeddings)

        verify {
            vertexService.embed("search_query")
            embeddingRepository.searchSimilar(SearchType.DATASET, vector, 0.3f, 7)
        }
    }

    @Test
    fun `delete text embedding by id`() {
        every { embeddingRepository.markDeletedByIdAndBeforeTimestamp(any(), any()) } returns Unit
        embeddingService.markDeletedByIdAndBeforeTimestamp("12345", 1612137600000L)
        verify {
            embeddingRepository.markDeletedByIdAndBeforeTimestamp("12345", 1612137600000L)
        }
    }
}
