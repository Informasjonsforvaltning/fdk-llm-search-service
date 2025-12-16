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
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(EmbeddingService::class.java)
    }

    /**
     * Store text embedding in the database as pgvector
     */
    open fun storeDatasetEmbedding(fdkId: String, dataset: Dataset, timestamp: Long) {
        // Check timestamp first to avoid expensive embedding generation for outdated messages
        if (!embeddingRepository.shouldProcessMessage(fdkId, timestamp)) {
            logger.debug("Skipped saving embedding for dataset {} - message timestamp {} is not newer than existing embedding", fdkId, timestamp)
            return
        }

        val themes =
            ((dataset.theme?.mapNotNull { it.title?.valueByPriority() ?: it.code } ?: emptyList()) +
            (dataset.losTheme?.mapNotNull { it.name?.valueByPriority() } ?: emptyList())).toSet()

        val formats = dataset.distribution?.flatMap { it.fdkFormat?.mapNotNull { format -> format.code }?.toSet() ?: emptySet() } ?: emptySet()
        val keywords = (dataset.keyword?.mapNotNull { it.valueByPriority() } ?: emptyList()).toSet()

        val issuedAndPeriodicity = if (dataset.issued != null || dataset.accrualPeriodicity?.prefLabel?.valueByPriority() != null) {
            val dt = dataset.issued?.let { issued ->
                val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

                runCatching {
                    LocalDateTime.parse(issued).format(formatter)
                }.getOrElse {
                    runCatching { LocalDate.parse(issued).format(formatter) }.getOrNull()
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
                        when {
                            it.startDate != null && it.endDate != null -> "${it.startDate} til ${it.endDate}"
                            it.startDate != null -> "fra ${it.startDate}"
                            it.endDate != null -> "til ${it.endDate}"
                            else -> ""
                        }}}."} else { "" } }
        """.trimIndent()

        embeddingRepository.saveEmbedding(fdkId, summary, vertexService.embed(summary).vector(), timestamp, mapOf(
            "type" to SearchType.DATASET.name,
            "title" to dataset.title?.valueByPriority(),
            "publisher" to dataset.publisher?.prefLabel?.valueByPriority(),
            "publisherId" to dataset.publisher?.id
        ))
    }

    /**
     * Store text embedding for Concept
     */
    open fun storeConceptEmbedding(fdkId: String, concept: Concept, timestamp: Long) {
        // Check timestamp first to avoid expensive embedding generation for outdated messages
        if (!embeddingRepository.shouldProcessMessage(fdkId, timestamp)) {
            logger.debug("Skipped saving embedding for concept {} - message timestamp {} is not newer than existing embedding", fdkId, timestamp)
            return
        }

        val keywords = ((concept.altLabel?.mapNotNull { it.valueByPriority() } ?: emptyList()) +
                (concept.hiddenLabel?.mapNotNull { it.valueByPriority() } ?: emptyList())).toSet()

        val summary = """
            Dette begrepet, med id '${fdkId}' og navn '${concept.prefLabel?.valueByPriority()}' er utgitt av '${concept.publisher?.prefLabel?.valueByPriority()}'.
            
            Beskrivelsen av begrepet er som følger:
            ${concept.definition?.text?.valueByPriority() ?: concept.prefLabel?.valueByPriority() ?: "Ingen beskrivelse tilgjengelig."}
            
            ${ if (keywords.isNotEmpty()) "Alternative navn for konseptet er: ${keywords.joinToString(", ")}." else ""}
        """.trimIndent()

        embeddingRepository.saveEmbedding(fdkId, summary, vertexService.embed(summary).vector(), timestamp, mapOf(
            "type" to SearchType.CONCEPT.name,
            "title" to concept.prefLabel?.valueByPriority(),
            "publisher" to concept.publisher?.prefLabel?.valueByPriority(),
            "publisherId" to concept.publisher?.id
        ))
    }

    /**
     * Store text embedding for DataService
     */
    open fun storeDataServiceEmbedding(fdkId: String, dataService: DataService, timestamp: Long) {
        // Check timestamp first to avoid expensive embedding generation for outdated messages
        if (!embeddingRepository.shouldProcessMessage(fdkId, timestamp)) {
            logger.debug("Skipped saving embedding for data service {} - message timestamp {} is not newer than existing embedding", fdkId, timestamp)
            return
        }

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

        embeddingRepository.saveEmbedding(fdkId, summary, vertexService.embed(summary).vector(), timestamp, mapOf(
            "type" to SearchType.DATA_SERVICE.name,
            "title" to dataService.title?.valueByPriority(),
            "publisher" to dataService.publisher?.prefLabel?.valueByPriority(),
            "publisherId" to dataService.publisher?.id
        ))
    }

    /**
     * Store text embedding for InformationModel
     */
    open fun storeInformationModelEmbedding(fdkId: String, informationModel: InformationModel, timestamp: Long) {
        // Check timestamp first to avoid expensive embedding generation for outdated messages
        if (!embeddingRepository.shouldProcessMessage(fdkId, timestamp)) {
            logger.debug("Skipped saving embedding for information model {} - message timestamp {} is not newer than existing embedding", fdkId, timestamp)
            return
        }

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

        embeddingRepository.saveEmbedding(fdkId, summary, vertexService.embed(summary).vector(), timestamp, mapOf(
            "type" to SearchType.INFORMATION_MODEL.name,
            "title" to informationModel.title?.valueByPriority(),
            "publisher" to informationModel.publisher?.prefLabel?.valueByPriority(),
            "publisherId" to informationModel.publisher?.id
        ))
    }

    /**
     * Store text embedding for Service
     */
    open fun storeServiceEmbedding(fdkId: String, service: no.digdir.fdk.search.llm.model.Service, timestamp: Long) {
        // Check timestamp first to avoid expensive embedding generation for outdated messages
        if (!embeddingRepository.shouldProcessMessage(fdkId, timestamp)) {
            logger.debug("Skipped saving embedding for service {} - message timestamp {} is not newer than existing embedding", fdkId, timestamp)
            return
        }

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
            ${ if (!service.spatial.isNullOrEmpty()) "Tjenesten er tilgjengelig i: ${service.spatial.joinToString(", ")}." else ""}
        """.trimIndent()

        embeddingRepository.saveEmbedding(fdkId, summary, vertexService.embed(summary).vector(), timestamp, mapOf(
            "type" to SearchType.SERVICE.name,
            "title" to service.title?.valueByPriority(),
            "publisher" to service.catalog?.publisher?.prefLabel?.valueByPriority(),
            "publisherId" to service.catalog?.publisher?.id
        ))
    }

    /**
     * Store text embedding for Event
     */
    open fun storeEventEmbedding(fdkId: String, event: Event, timestamp: Long) {
        // Check timestamp first to avoid expensive embedding generation for outdated messages
        if (!embeddingRepository.shouldProcessMessage(fdkId, timestamp)) {
            logger.debug("Skipped saving embedding for event {} - message timestamp {} is not newer than existing embedding", fdkId, timestamp)
            return
        }

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

        embeddingRepository.saveEmbedding(fdkId, summary, vertexService.embed(summary).vector(), timestamp, mapOf(
            "type" to SearchType.EVENT.name,
            "title" to event.title?.valueByPriority(),
            "publisher" to event.catalog?.publisher?.prefLabel?.valueByPriority(),
            "publisherId" to event.catalog?.publisher?.id
        ))
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
}
