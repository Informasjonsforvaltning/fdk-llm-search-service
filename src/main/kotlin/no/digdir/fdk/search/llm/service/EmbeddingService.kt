package no.digdir.fdk.search.llm.service

import no.digdir.fdk.search.llm.model.Dataset
import no.digdir.fdk.search.llm.model.TextEmbedding
import no.digdir.fdk.search.llm.model.SearchType
import no.digdir.fdk.search.llm.model.valueByPriority
import no.digdir.fdk.search.llm.repository.EmbeddingRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


@Service
open class EmbeddingService(
    private val vertexService: VertexService,
    private val embeddingRepository: EmbeddingRepository
) {
    /**
     * Store text embedding in the database as pgvector
     */
    open fun storeDatasetEmbedding(fdkId: String, dataset: Dataset, timestamp: Long) {
        val themes =
            ((dataset.theme?.mapNotNull { it.title?.valueByPriority() ?: it.code } ?: emptyList()) +
            (dataset.losTheme?.mapNotNull { it.name?.valueByPriority() } ?: emptyList())).toSet()

        val formats = dataset.distribution?.flatMap { it.fdkFormat?.mapNotNull { format -> format.code }?.toSet() ?: emptySet() } ?: emptySet()
        val keywords = (dataset.keyword?.mapNotNull { it.valueByPriority() } ?: emptyList()).toSet()

        val issuedAndPeriodicity = if (dataset.issued != null || dataset.accrualPeriodicity?.prefLabel?.valueByPriority() != null) {
            val dt = dataset.issued?.let {
                try {
                    LocalDateTime.parse(it).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                } catch (e: Exception) {
                    LocalDate.parse(it).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                }
            }
            val periodicity = dataset.accrualPeriodicity?.prefLabel?.valueByPriority()
            "Datasettet ${ if (dt != null) "ble utgitt $dt" else "" }" +
                (if(dt != null && periodicity != null) " og " else "") +
                (if(periodicity != null) "oppdateres $periodicity" else "") +
                "."
        } else ""

        val summary = """
            Dette datasettet, med id '${fdkId}' og navn '${dataset.title?.valueByPriority()}' er utgitt av '${dataset.publisher?.prefLabel?.valueByPriority()}'.
            Datasettet har ${dataset.accessRights?.prefLabel?.valueByPriority()?.lowercase() ?: "ukjent"} tilgang.
            
            Beskrivelsen av datasettet er som følger:
            ${dataset.description?.valueByPriority() ?: "Ingen beskrivelse tilgjengelig."}
             
            $issuedAndPeriodicity
            Datasettet har ${dataset.distribution?.size ?: 0} distribusjoner${ if (formats.isNotEmpty()) " og tilbyr data på formatene ${formats.joinToString(", ")}" else ""}.
            ${ if (themes.isNotEmpty()) "Temaene for datasettet er: ${themes.joinToString(", ")}." else ""}
            ${ if (keywords.isNotEmpty()) "Nøkkelordene for datasettet er: ${keywords.joinToString(", ")}." else ""}
            ${ if (!dataset.temporal.isNullOrEmpty()) {
                "Dataen er tidsmessig begrenset: ${dataset.temporal.joinToString(", ") {
                    "${it.startDate} til ${it.endDate}"}}."} else { "" } }
        """.trimIndent()

        embeddingRepository.saveEmbedding(fdkId, summary, vertexService.embed(summary).vector(), timestamp, mapOf(
            "type" to SearchType.DATASET.name,
            "title" to dataset.title?.valueByPriority(),
            "publisher" to dataset.publisher?.prefLabel?.valueByPriority(),
            "publisherId" to dataset.publisher?.id
        ))
    }

    /**
     * Perform similarity search
     */
    fun similaritySearch(query: String, type: SearchType, simThreshold: Float, numMatches: Int): List<TextEmbedding> {
        val embedding = vertexService.embed(query)
        return embeddingRepository.searchSimilar(type, embedding.vector(), simThreshold, numMatches)
    }

    /**
     * Delete embedding by id
     */
    fun markDeletedByIdAndBeforeTimestamp(id: String, timestamp: Long) {
        embeddingRepository.markDeletedByIdAndBeforeTimestamp(id, timestamp)
    }
}
