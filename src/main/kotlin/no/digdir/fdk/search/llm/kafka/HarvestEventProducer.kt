package no.digdir.fdk.search.llm.kafka

import no.fdk.harvest.DataType
import no.fdk.harvest.HarvestEvent
import no.fdk.harvest.HarvestPhase
import no.fdk.rdf.parse.RdfParseResourceType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class HarvestEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, HarvestEvent>
) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(HarvestEventProducer::class.java)
    }

    /**
     * Map RdfParseResourceType to DataType enum
     */
    private fun mapResourceTypeToDataType(resourceType: RdfParseResourceType): DataType {
        return when (resourceType) {
            RdfParseResourceType.DATASET -> DataType.dataset
            RdfParseResourceType.DATA_SERVICE -> DataType.dataservice
            RdfParseResourceType.CONCEPT -> DataType.concept
            RdfParseResourceType.INFORMATION_MODEL -> DataType.informationmodel
            RdfParseResourceType.SERVICE -> DataType.publicService
            RdfParseResourceType.EVENT -> DataType.event
        }
    }

    /**
     * Map resource type string to DataType enum
     */
    fun mapResourceTypeStringToDataType(resourceType: String): DataType {
        return when (resourceType.lowercase()) {
            "dataset" -> DataType.dataset
            "data-service" -> DataType.dataservice
            "concept" -> DataType.concept
            "information-model" -> DataType.informationmodel
            "service" -> DataType.publicService
            "event" -> DataType.event
            else -> throw IllegalArgumentException("Unknown resource type: $resourceType")
        }
    }

    /**
     * Produce harvest event when search indexing finishes successfully
     */
    fun produceSuccessEvent(
        harvestRunId: String?,
        uri: String?,
        resourceType: RdfParseResourceType,
        fdkId: String,
        startTime: Instant,
        endTime: Instant
    ) {
        if (harvestRunId == null) {
            logger.debug("Skipping harvest event - harvestRunId is null for fdkId: $fdkId")
            return
        }

        try {
            val event = HarvestEvent.newBuilder()
                .setPhase(HarvestPhase.AI_SEARCH_PROCESSING)
                .setRunId(harvestRunId)
                .setDataType(mapResourceTypeToDataType(resourceType))
                .setFdkId(fdkId)
                .setResourceUri(uri)
                .setTimestamp(endTime.toEpochMilli())
                .setStartTime(startTime.toString())
                .setEndTime(endTime.toString())
                .setDataSourceId(null)
                .setDataSourceUrl(null)
                .setAcceptHeader(null)
                .setErrorMessage(null)
                .setChangedResourcesCount(null)
                .setUnchangedResourcesCount(null)
                .setRemovedResourcesCount(null)
                .build()

            kafkaTemplate.send("harvest-events", harvestRunId, event)
            logger.debug("Produced harvest success event for fdkId: $fdkId, harvestRunId: $harvestRunId")
        } catch (e: Exception) {
            logger.error("Error producing harvest success event for fdkId: $fdkId", e)
        }
    }

    /**
     * Produce harvest event when search indexing fails
     */
    fun produceFailureEvent(
        harvestRunId: String?,
        uri: String?,
        resourceType: RdfParseResourceType,
        fdkId: String,
        startTime: Instant,
        endTime: Instant,
        errorMessage: String
    ) {
        if (harvestRunId == null) {
            logger.debug("Skipping harvest event - harvestRunId is null for fdkId: $fdkId")
            return
        }

        try {
            val event = HarvestEvent.newBuilder()
                .setPhase(HarvestPhase.AI_SEARCH_PROCESSING)
                .setRunId(harvestRunId)
                .setDataType(mapResourceTypeToDataType(resourceType))
                .setFdkId(fdkId)
                .setResourceUri(uri)
                .setTimestamp(endTime.toEpochMilli())
                .setStartTime(startTime.toString())
                .setEndTime(endTime.toString())
                .setErrorMessage(errorMessage)
                .setDataSourceId(null)
                .setDataSourceUrl(null)
                .setAcceptHeader(null)
                .setChangedResourcesCount(null)
                .setUnchangedResourcesCount(null)
                .setRemovedResourcesCount(null)
                .build()

            kafkaTemplate.send("harvest-events", harvestRunId, event)
            logger.debug("Produced harvest failure event for fdkId: $fdkId, harvestRunId: $harvestRunId")
        } catch (e: Exception) {
            logger.error("Error producing harvest failure event for fdkId: $fdkId", e)
        }
    }

    /**
     * Produce harvest event when resource deletion finishes successfully
     */
    fun produceDeletionSuccessEvent(
        harvestRunId: String?,
        uri: String?,
        dataType: DataType,
        fdkId: String,
        startTime: Instant,
        endTime: Instant
    ) {
        if (harvestRunId == null) {
            logger.debug("Skipping harvest event - harvestRunId is null for fdkId: $fdkId")
            return
        }

        try {
            val event = HarvestEvent.newBuilder()
                .setPhase(HarvestPhase.AI_SEARCH_PROCESSING)
                .setRunId(harvestRunId)
                .setDataType(dataType)
                .setFdkId(fdkId)
                .setResourceUri(uri)
                .setTimestamp(endTime.toEpochMilli())
                .setStartTime(startTime.toString())
                .setEndTime(endTime.toString())
                .setDataSourceId(null)
                .setDataSourceUrl(null)
                .setAcceptHeader(null)
                .setErrorMessage(null)
                .setChangedResourcesCount(null)
                .setUnchangedResourcesCount(null)
                .setRemovedResourcesCount(null)
                .build()

            kafkaTemplate.send("harvest-events", harvestRunId, event)
            logger.debug("Produced harvest deletion success event for fdkId: $fdkId, harvestRunId: $harvestRunId")
        } catch (e: Exception) {
            logger.error("Error producing harvest deletion success event for fdkId: $fdkId", e)
        }
    }

    /**
     * Produce harvest event when resource deletion fails
     */
    fun produceDeletionFailureEvent(
        harvestRunId: String?,
        uri: String?,
        dataType: DataType,
        fdkId: String,
        startTime: Instant,
        endTime: Instant,
        errorMessage: String
    ) {
        if (harvestRunId == null) {
            logger.debug("Skipping harvest event - harvestRunId is null for fdkId: $fdkId")
            return
        }

        try {
            val event = HarvestEvent.newBuilder()
                .setPhase(HarvestPhase.AI_SEARCH_PROCESSING)
                .setRunId(harvestRunId)
                .setDataType(dataType)
                .setFdkId(fdkId)
                .setResourceUri(uri)
                .setTimestamp(endTime.toEpochMilli())
                .setStartTime(startTime.toString())
                .setEndTime(endTime.toString())
                .setErrorMessage(errorMessage)
                .setDataSourceId(null)
                .setDataSourceUrl(null)
                .setAcceptHeader(null)
                .setChangedResourcesCount(null)
                .setUnchangedResourcesCount(null)
                .setRemovedResourcesCount(null)
                .build()

            kafkaTemplate.send("harvest-events", harvestRunId, event)
            logger.debug("Produced harvest deletion failure event for fdkId: $fdkId, harvestRunId: $harvestRunId")
        } catch (e: Exception) {
            logger.error("Error producing harvest deletion failure event for fdkId: $fdkId", e)
        }
    }
}

