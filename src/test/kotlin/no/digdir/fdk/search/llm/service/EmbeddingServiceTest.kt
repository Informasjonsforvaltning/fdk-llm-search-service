package no.digdir.fdk.search.llm.service

import dev.langchain4j.data.embedding.Embedding
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.digdir.fdk.search.llm.model.SearchType
import no.digdir.fdk.search.llm.model.TextEmbedding
import no.digdir.fdk.search.llm.repository.EmbeddingRepository
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

        every { embeddingRepository.shouldProcessMessage(any(), any()) } returns true
        every { vertexService.embed(any()) } returns Embedding(vector)
        every { embeddingRepository.saveEmbedding(any(), any(), any(), any(), any()) } returns true

        embeddingService.storeDatasetEmbedding("12345", TEST_DATASET_ALL_FIELDS, 1612137600000L)

        verify {
            embeddingRepository.shouldProcessMessage("12345", 1612137600000L)
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
        val embeddings = embeddingService.similaritySearch("search_query", SearchType.DATASET, 0.3f, 10)

        assertEquals(2, embeddings.size)
        assertEquals(expectedEmbeddings, embeddings)

        verify {
            vertexService.embed("search_query")
            embeddingRepository.searchSimilar(SearchType.DATASET, vector, 0.3f, 10)
        }
    }

    @Test
    fun `store concept embedding`() {
        val vector = FloatArray(768)
        vector.fill(0.1f)

        val expectedSummary = """
Dette begrepet, med id 'concept-123' og navn 'NB Concept Label' er utgitt av 'NB Concept Publisher'.

Beskrivelsen av begrepet er som følger:
NB Concept definition

Alternative navn for konseptet er: NB Alt Label, NB Hidden Label.
        """.trimIndent()

        every { embeddingRepository.shouldProcessMessage(any(), any()) } returns true
        every { vertexService.embed(any()) } returns Embedding(vector)
        every { embeddingRepository.saveEmbedding(any(), any(), any(), any(), any()) } returns true

        embeddingService.storeConceptEmbedding("concept-123", TEST_CONCEPT_ALL_FIELDS, 1612137600000L)

        verify {
            embeddingRepository.saveEmbedding(
                "concept-123", expectedSummary, vector, 1612137600000, mapOf(
                    "type" to SearchType.CONCEPT.name,
                    "title" to "NB Concept Label",
                    "publisher" to "NB Concept Publisher",
                    "publisherId" to "concept-pub-id"
                )
            )
        }
    }

    @Test
    fun `store data service embedding`() {
        val vector = FloatArray(768)
        vector.fill(0.1f)

        val expectedSummary = """
Denne datatjenesten, med id 'dataservice-123' og navn 'NB DataService Title' er utgitt av 'NB DataService Publisher'.
Datatjenesten har public tilgang.

Beskrivelsen av datatjenesten er som følger:
NB DataService description

Datatjenesten tilbyr data på formatene json.
Temaene for datatjenesten er: theme1, lostheme1.
Nøkkelordene for datatjenesten er: keyword1.
Datatjenesten betjener 2 datasett.
        """.trimIndent()

        every { embeddingRepository.shouldProcessMessage(any(), any()) } returns true
        every { vertexService.embed(any()) } returns Embedding(vector)
        every { embeddingRepository.saveEmbedding(any(), any(), any(), any(), any()) } returns true

        embeddingService.storeDataServiceEmbedding("dataservice-123", TEST_DATA_SERVICE_ALL_FIELDS, 1612137600000L)

        verify {
            embeddingRepository.saveEmbedding(
                "dataservice-123", expectedSummary, vector, 1612137600000, mapOf(
                    "type" to SearchType.DATA_SERVICE.name,
                    "title" to "NB DataService Title",
                    "publisher" to "NB DataService Publisher",
                    "publisherId" to "dataservice-pub-id"
                )
            )
        }
    }

    @Test
    fun `store information model embedding`() {
        val vector = FloatArray(768)
        vector.fill(0.1f)

        val expectedSummary = """
Denne informasjonsmodellen, med id 'infomodel-123' og navn 'NB InformationModel Title' er utgitt av 'NB InformationModel Publisher'.
Informasjonsmodellen har public tilgang.

Beskrivelsen av informasjonsmodellen er som følger:
NB InformationModel description

Temaene for informasjonsmodellen er: theme1, lostheme1.
Nøkkelordene for informasjonsmodellen er: keyword1.
Informasjonsmodellen omhandler emnene: subject1, subject2.
        """.trimIndent()

        every { embeddingRepository.shouldProcessMessage(any(), any()) } returns true
        every { vertexService.embed(any()) } returns Embedding(vector)
        every { embeddingRepository.saveEmbedding(any(), any(), any(), any(), any()) } returns true

        embeddingService.storeInformationModelEmbedding("infomodel-123", TEST_INFORMATION_MODEL_ALL_FIELDS, 1612137600000L)

        verify {
            embeddingRepository.saveEmbedding(
                "infomodel-123", expectedSummary, vector, 1612137600000, mapOf(
                    "type" to SearchType.INFORMATION_MODEL.name,
                    "title" to "NB InformationModel Title",
                    "publisher" to "NB InformationModel Publisher",
                    "publisherId" to "infomodel-pub-id"
                )
            )
        }
    }

    @Test
    fun `store service embedding`() {
        val vector = FloatArray(768)
        vector.fill(0.1f)

        val expectedSummary = """
Denne tjenesten, med id 'service-123' og navn 'NB Service Title' er utgitt av 'NB Catalog Publisher'.

Beskrivelsen av tjenesten er som følger:
NB Service description

Tjenesten eies av: NB Owner Name.
Tjenesten har kompetent myndighet: NB Authority Name.
Temaene for tjenesten er: theme1, lostheme1.
Nøkkelordene for tjenesten er: keyword1.
        """.trimIndent()

        every { embeddingRepository.shouldProcessMessage(any(), any()) } returns true
        every { vertexService.embed(any()) } returns Embedding(vector)
        every { embeddingRepository.saveEmbedding(any(), any(), any(), any(), any()) } returns true

        embeddingService.storeServiceEmbedding("service-123", TEST_SERVICE_ALL_FIELDS, 1612137600000L)

        verify {
            embeddingRepository.saveEmbedding(
                "service-123", expectedSummary, vector, 1612137600000, mapOf(
                    "type" to SearchType.SERVICE.name,
                    "title" to "NB Service Title",
                    "publisher" to "NB Catalog Publisher",
                    "publisherId" to "catalog-pub-id"
                )
            )
        }
    }

    @Test
    fun `store event embedding`() {
        val vector = FloatArray(768)
        vector.fill(0.1f)

        val expectedSummary = """
Denne hendelsen, med id 'event-123' og navn 'NB Event Title' er utgitt av 'NB Catalog Publisher'.
Hendelsen er en livshendelse.

Beskrivelsen av hendelsen er som følger:
NB Event description

Hendelsen omhandler emnene: subject1, subject2.
        """.trimIndent()

        every { embeddingRepository.shouldProcessMessage(any(), any()) } returns true
        every { vertexService.embed(any()) } returns Embedding(vector)
        every { embeddingRepository.saveEmbedding(any(), any(), any(), any(), any()) } returns true

        embeddingService.storeEventEmbedding("event-123", TEST_EVENT_ALL_FIELDS, 1612137600000L)

        verify {
            embeddingRepository.saveEmbedding(
                "event-123", expectedSummary, vector, 1612137600000, mapOf(
                    "type" to SearchType.EVENT.name,
                    "title" to "NB Event Title",
                    "publisher" to "NB Catalog Publisher",
                    "publisherId" to "catalog-pub-id"
                )
            )
        }
    }

    @Test
    fun `delete text embedding by id`() {
        every { embeddingRepository.markDeletedByIdAndBeforeTimestamp(any(), any()) } returns true
        embeddingService.markDeletedByIdAndBeforeTimestamp("12345", 1612137600000L)
        verify {
            embeddingRepository.markDeletedByIdAndBeforeTimestamp("12345", 1612137600000L)
        }
    }

    @Test
    fun `store dataset embedding should skip when timestamp is older`() {
        every { embeddingRepository.shouldProcessMessage("12345", 1612137600000L) } returns false

        embeddingService.storeDatasetEmbedding("12345", TEST_DATASET_ALL_FIELDS, 1612137600000L)

        verify {
            embeddingRepository.shouldProcessMessage("12345", 1612137600000L)
        }
        verify(exactly = 0) {
            vertexService.embed(any())
            embeddingRepository.saveEmbedding(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `store dataset embedding should process when timestamp is newer`() {
        val vector = FloatArray(768)
        vector.fill(0.1f)

        every { embeddingRepository.shouldProcessMessage("12345", 1612137600001L) } returns true
        every { vertexService.embed(any()) } returns Embedding(vector)
        every { embeddingRepository.saveEmbedding(any(), any(), any(), any(), any()) } returns true

        embeddingService.storeDatasetEmbedding("12345", TEST_DATASET_ALL_FIELDS, 1612137600001L)

        verify {
            embeddingRepository.shouldProcessMessage("12345", 1612137600001L)
            vertexService.embed(any())
            embeddingRepository.saveEmbedding(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `store concept embedding should skip when timestamp is older`() {
        every { embeddingRepository.shouldProcessMessage("concept-123", 1612137600000L) } returns false

        embeddingService.storeConceptEmbedding("concept-123", TEST_CONCEPT_ALL_FIELDS, 1612137600000L)

        verify {
            embeddingRepository.shouldProcessMessage("concept-123", 1612137600000L)
        }
        verify(exactly = 0) {
            vertexService.embed(any())
            embeddingRepository.saveEmbedding(any(), any(), any(), any(), any())
        }
    }
}
