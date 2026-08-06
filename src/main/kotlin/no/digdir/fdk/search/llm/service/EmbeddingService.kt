package no.digdir.fdk.search.llm.service

import no.digdir.fdk.search.llm.model.*
import no.digdir.fdk.search.llm.repository.EmbeddingRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
        if (!shouldProcess(fdkId, timestamp, "dataset")) return

        val themes =
            ((dataset.theme?.mapNotNull { it.title?.valueByPriority() ?: it.code } ?: emptyList()) +
            (dataset.losTheme?.mapNotNull { it.name?.valueByPriority() } ?: emptyList())).toSet()

        val formats = dataset.distribution?.flatMap { it.fdkFormat?.mapNotNull { format -> format.code }?.toSet() ?: emptySet() } ?: emptySet()
        val keywords = (dataset.keyword?.mapNotNull { it.valueByPriority() } ?: emptyList()).toSet()
        val issuedAndPeriodicity = formatDatasetIssuedAndPeriodicity(dataset)
        val temporalSummary = formatTemporalRanges(dataset.temporal)

        val summary = """
            Dette datasettet, med id '${fdkId}' og navn '${dataset.title?.valueByPriority()}' er utgitt av '${dataset.publisher?.prefLabel?.valueByPriority()}'.
            Datasettet har ${dataset.accessRights?.prefLabel?.valueByPriority()?.lowercase() ?: "ukjent"} tilgang.
            
            Beskrivelsen av datasettet er som følger:
            ${dataset.description?.valueByPriority() ?: "Ingen beskrivelse tilgjengelig."}
             
            $issuedAndPeriodicity
            Datasettet har ${dataset.distribution?.size ?: 0} distribusjoner${ if (formats.isNotEmpty()) " og tilbyr data på formatene ${formats.joinToString(", ")}" else ""}.
            ${ if (themes.isNotEmpty()) "Temaene for datasettet er: ${themes.joinToString(", ")}." else ""}
            ${ if (keywords.isNotEmpty()) "Nøkkelordene for datasettet er: ${keywords.joinToString(", ")}." else ""}
            $temporalSummary
        """.trimIndent()

        saveEmbedding(
            fdkId,
            summary,
            timestamp,
            buildMetadata(
                SearchType.DATASET,
                dataset.title?.valueByPriority(),
                dataset.publisher?.prefLabel?.valueByPriority(),
                dataset.publisher?.id,
            ),
        )
    }

    /**
     * Store text embedding for Concept
     */
    open fun storeConceptEmbedding(fdkId: String, concept: Concept, timestamp: Long) {
        if (!shouldProcess(fdkId, timestamp, "concept")) return

        val keywords = ((concept.altLabel?.mapNotNull { it.valueByPriority() } ?: emptyList()) +
                (concept.hiddenLabel?.mapNotNull { it.valueByPriority() } ?: emptyList())).toSet()

        val summary = """
            Dette begrepet, med id '${fdkId}' og navn '${concept.prefLabel?.valueByPriority()}' er utgitt av '${concept.publisher?.prefLabel?.valueByPriority()}'.
            
            Beskrivelsen av begrepet er som følger:
            ${concept.definition?.text?.valueByPriority() ?: concept.prefLabel?.valueByPriority() ?: "Ingen beskrivelse tilgjengelig."}
            
            ${ if (keywords.isNotEmpty()) "Alternative navn for konseptet er: ${keywords.joinToString(", ")}." else ""}
        """.trimIndent()

        saveEmbedding(
            fdkId,
            summary,
            timestamp,
            buildMetadata(
                SearchType.CONCEPT,
                concept.prefLabel?.valueByPriority(),
                concept.publisher?.prefLabel?.valueByPriority(),
                concept.publisher?.id,
            ),
        )
    }

    /**
     * Store text embedding for DataService
     */
    open fun storeDataServiceEmbedding(fdkId: String, dataService: DataService, timestamp: Long) {
        if (!shouldProcess(fdkId, timestamp, "data service")) return

        val themes =
            ((dataService.theme?.mapNotNull { it.title?.valueByPriority() ?: it.code } ?: emptyList()) +
            (dataService.losTheme?.mapNotNull { it.name?.valueByPriority() } ?: emptyList())).toSet()

        val formats = dataService.fdkFormat?.mapNotNull { it.code }?.toSet() ?: emptySet()
        val keywords = (dataService.keyword?.mapNotNull { it.valueByPriority() } ?: emptyList()).toSet()

        val summary = """
            Denne datatjenesten, med id '${fdkId}' og navn '${dataService.title?.valueByPriority()}' er utgitt av '${dataService.publisher?.prefLabel?.valueByPriority()}'.
            Datatjenesten har ${dataService.accessRights?.prefLabel?.valueByPriority()?.lowercase() ?: "ukjent"} tilgang.
            
            Beskrivelsen av datatjenesten er som følger:
            ${dataService.description?.valueByPriority() ?: "Ingen beskrivelse tilgjengelig."}
            
            ${ if (formats.isNotEmpty()) "Datatjenesten tilbyr data på formatene ${formats.joinToString(", ")}." else ""}
            ${ if (themes.isNotEmpty()) "Temaene for datatjenesten er: ${themes.joinToString(", ")}." else ""}
            ${ if (keywords.isNotEmpty()) "Nøkkelordene for datatjenesten er: ${keywords.joinToString(", ")}." else ""}
            ${ if (!dataService.servesDataset.isNullOrEmpty()) "Datatjenesten betjener ${dataService.servesDataset.size} datasett." else ""}
        """.trimIndent()

        saveEmbedding(
            fdkId,
            summary,
            timestamp,
            buildMetadata(
                SearchType.DATA_SERVICE,
                dataService.title?.valueByPriority(),
                dataService.publisher?.prefLabel?.valueByPriority(),
                dataService.publisher?.id,
            ),
        )
    }

    /**
     * Store text embedding for InformationModel
     */
    open fun storeInformationModelEmbedding(fdkId: String, informationModel: InformationModel, timestamp: Long) {
        if (!shouldProcess(fdkId, timestamp, "information model")) return

        val themes =
            ((informationModel.theme?.mapNotNull { it.title?.valueByPriority() ?: it.code } ?: emptyList()) +
            (informationModel.losTheme?.mapNotNull { it.name?.valueByPriority() } ?: emptyList())).toSet()

        val keywords = (informationModel.keyword?.mapNotNull { it.valueByPriority() } ?: emptyList()).toSet()

        val summary = """
            Denne informasjonsmodellen, med id '${fdkId}' og navn '${informationModel.title?.valueByPriority()}' er utgitt av '${informationModel.publisher?.prefLabel?.valueByPriority()}'.
            Informasjonsmodellen har ${informationModel.accessRights?.prefLabel?.valueByPriority()?.lowercase() ?: "ukjent"} tilgang.
            
            Beskrivelsen av informasjonsmodellen er som følger:
            ${informationModel.description?.valueByPriority() ?: "Ingen beskrivelse tilgjengelig."}
            
            ${ if (themes.isNotEmpty()) "Temaene for informasjonsmodellen er: ${themes.joinToString(", ")}." else ""}
            ${ if (keywords.isNotEmpty()) "Nøkkelordene for informasjonsmodellen er: ${keywords.joinToString(", ")}." else ""}
            ${ if (!informationModel.subjects.isNullOrEmpty()) "Informasjonsmodellen omhandler emnene: ${informationModel.subjects.joinToString(", ")}." else ""}
        """.trimIndent()

        saveEmbedding(
            fdkId,
            summary,
            timestamp,
            buildMetadata(
                SearchType.INFORMATION_MODEL,
                informationModel.title?.valueByPriority(),
                informationModel.publisher?.prefLabel?.valueByPriority(),
                informationModel.publisher?.id,
            ),
        )
    }

    /**
     * Store text embedding for Service
     */
    open fun storeServiceEmbedding(fdkId: String, service: ServiceResource, timestamp: Long) {
        if (!shouldProcess(fdkId, timestamp, "service")) return

        val themes =
            ((service.euDataThemes?.mapNotNull { it.title?.valueByPriority() ?: it.code } ?: emptyList()) +
            (service.losTheme?.mapNotNull { it.name?.valueByPriority() } ?: emptyList())).toSet()

        val keywords = (service.keyword?.mapNotNull { it.valueByPriority() } ?: emptyList()).toSet()
        val owners = service.ownedBy?.mapNotNull { it.prefLabel?.valueByPriority() ?: it.title?.valueByPriority() } ?: emptyList()
        val authorities = service.hasCompetentAuthority?.mapNotNull { it.prefLabel?.valueByPriority() ?: it.title?.valueByPriority() } ?: emptyList()

        val summary = """
            Denne tjenesten, med id '${fdkId}' og navn '${service.title?.valueByPriority()}' er utgitt av '${service.catalog?.publisher?.prefLabel?.valueByPriority()}'.
            
            Beskrivelsen av tjenesten er som følger:
            ${service.description?.valueByPriority() ?: "Ingen beskrivelse tilgjengelig."}
            
            ${ if (owners.isNotEmpty()) "Tjenesten eies av: ${owners.joinToString(", ")}." else ""}
            ${ if (authorities.isNotEmpty()) "Tjenesten har kompetent myndighet: ${authorities.joinToString(", ")}." else ""}
            ${ if (themes.isNotEmpty()) "Temaene for tjenesten er: ${themes.joinToString(", ")}." else ""}
            ${ if (keywords.isNotEmpty()) "Nøkkelordene for tjenesten er: ${keywords.joinToString(", ")}." else ""}
        """.trimIndent()

        saveEmbedding(
            fdkId,
            summary,
            timestamp,
            buildMetadata(
                SearchType.SERVICE,
                service.title?.valueByPriority(),
                service.catalog?.publisher?.prefLabel?.valueByPriority(),
                service.catalog?.publisher?.id,
            ),
        )
    }

    /**
     * Store text embedding for Event
     */
    open fun storeEventEmbedding(fdkId: String, event: Event, timestamp: Long) {
        if (!shouldProcess(fdkId, timestamp, "event")) return

        val specializedType = event.specializedType?.let {
            when (it) {
                "LIFE_EVENT" -> "livshendelse"
                "BUSINESS_EVENT" -> "virksomhetshendelse"
                else -> it.lowercase()
            }
        }

        val summary = """
            Denne hendelsen, med id '${fdkId}' og navn '${event.title?.valueByPriority()}' er utgitt av '${event.catalog?.publisher?.prefLabel?.valueByPriority()}'.
            ${ if (specializedType != null) "Hendelsen er en $specializedType." else ""}
            
            Beskrivelsen av hendelsen er som følger:
            ${event.description?.valueByPriority() ?: "Ingen beskrivelse tilgjengelig."}
            
            ${ if (!event.subject.isNullOrEmpty()) "Hendelsen omhandler emnene: ${event.subject.joinToString(", ")}." else ""}
        """.trimIndent()

        saveEmbedding(
            fdkId,
            summary,
            timestamp,
            buildMetadata(
                SearchType.EVENT,
                event.title?.valueByPriority(),
                event.catalog?.publisher?.prefLabel?.valueByPriority(),
                event.catalog?.publisher?.id,
            ),
        )
    }

    /**
     * Perform similarity search
     * @param type If null, searches across all resource types. Otherwise filters by the specified type.
     */
    fun similaritySearch(query: String, type: SearchType?, simThreshold: Float, numMatches: Int): List<TextEmbedding> {
        val embedding = vertexService.embed(query)
        return embeddingRepository.searchSimilar(type, embedding.vector(), simThreshold, numMatches)
    }

    /**
     * Mark embedding as deleted by id
     * Only processes if the message timestamp is newer than the existing embedding timestamp.
     *
     * @return true if the embedding was marked as deleted, false if skipped due to older timestamp
     */
    fun markDeletedByIdAndBeforeTimestamp(id: String, timestamp: Long): Boolean {
        val deleted = embeddingRepository.markDeletedByIdAndBeforeTimestamp(id, timestamp)
        if (!deleted) {
            logger.debug("Skipped marking embedding as deleted for {} - message timestamp {} is not newer than existing embedding", id, timestamp)
        }
        return deleted
    }

    private fun shouldProcess(fdkId: String, timestamp: Long, resourceLabel: String): Boolean {
        if (!embeddingRepository.shouldProcessMessage(fdkId, timestamp)) {
            logger.debug(
                "Skipped saving embedding for {} {} - message timestamp {} is not newer than existing embedding",
                resourceLabel,
                fdkId,
                timestamp,
            )
            return false
        }
        return true
    }

    private fun buildMetadata(
        type: SearchType,
        title: String?,
        publisherLabel: String?,
        publisherId: String?,
    ): Map<String, String?> = mapOf(
        "type" to type.name,
        "title" to title,
        "publisher" to publisherLabel,
        "publisherId" to publisherId,
    )

    private fun saveEmbedding(
        fdkId: String,
        summary: String,
        timestamp: Long,
        metadata: Map<String, String?>,
    ) {
        embeddingRepository.saveEmbedding(
            fdkId,
            summary,
            vertexService.embed(summary).vector(),
            timestamp,
            metadata,
        )
    }

    private fun formatDatasetIssuedAndPeriodicity(dataset: Dataset): String {
        if (dataset.issued == null && dataset.accrualPeriodicity?.prefLabel?.valueByPriority() == null) {
            return ""
        }

        val issuedDate = dataset.issued?.let(::formatIssuedDate)
        val periodicity = dataset.accrualPeriodicity?.prefLabel?.valueByPriority()
        return "Datasettet ${ if (issuedDate != null) "ble utgitt $issuedDate" else "" }" +
            (if (issuedDate != null && periodicity != null) " og " else "") +
            (if (periodicity != null) "oppdateres $periodicity" else "") +
            "."
    }

    private fun formatIssuedDate(issued: String): String? {
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        return runCatching {
            LocalDateTime.parse(issued).format(formatter)
        }.getOrElse {
            runCatching { LocalDate.parse(issued).format(formatter) }.getOrNull()
        }
    }

    private fun formatTemporalRanges(temporal: List<Temporal>?): String {
        if (temporal.isNullOrEmpty()) return ""

        val ranges = temporal.joinToString(", ") { range ->
            when {
                range.startDate != null && range.endDate != null -> "${range.startDate} til ${range.endDate}"
                range.startDate != null -> "fra ${range.startDate}"
                range.endDate != null -> "til ${range.endDate}"
                else -> ""
            }
        }
        return "Dataen er tidsmessig begrenset: $ranges."
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(EmbeddingService::class.java)
    }
}
