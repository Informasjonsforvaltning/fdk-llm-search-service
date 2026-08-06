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
    /**
     * Map RdfParseResourceType to DataType enum
     */
    internal fun mapResourceTypeToDataType(resourceType: RdfParseResourceType): DataType {
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
        sendHarvestEvent(harvestRunId, fdkId, "harvest success event") { runId ->
            buildHarvestEvent(
                harvestRunId = runId,
                dataType = mapResourceTypeToDataType(resourceType),
                fdkId = fdkId,
                uri = uri,
                startTime = startTime,
                endTime = endTime,
            )
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
        sendHarvestEvent(harvestRunId, fdkId, "harvest failure event") { runId ->
            buildHarvestEvent(
                harvestRunId = runId,
                dataType = mapResourceTypeToDataType(resourceType),
                fdkId = fdkId,
                uri = uri,
                startTime = startTime,
                endTime = endTime,
                errorMessage = errorMessage,
            )
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
        sendHarvestEvent(harvestRunId, fdkId, "harvest deletion success event") { runId ->
            buildHarvestEvent(
                harvestRunId = runId,
                dataType = dataType,
                fdkId = fdkId,
                uri = uri,
                startTime = startTime,
                endTime = endTime,
            )
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
        sendHarvestEvent(harvestRunId, fdkId, "harvest deletion failure event") { runId ->
            buildHarvestEvent(
                harvestRunId = runId,
                dataType = dataType,
                fdkId = fdkId,
                uri = uri,
                startTime = startTime,
                endTime = endTime,
                errorMessage = errorMessage,
            )
        }
    }

    private fun buildHarvestEvent(
        harvestRunId: String,
        dataType: DataType,
        fdkId: String,
        uri: String?,
        startTime: Instant,
        endTime: Instant,
        errorMessage: String? = null,
    ): HarvestEvent = HarvestEvent.newBuilder()
        .setPhase(HarvestPhase.AI_SEARCH_PROCESSING)
        .setRunId(harvestRunId)
        .setDataType(dataType)
        .setFdkId(fdkId)
        .setResourceUri(uri)
        .setStartTime(startTime.toString())
        .setEndTime(endTime.toString())
        .setErrorMessage(errorMessage)
        .setDataSourceId(null)
        .setDataSourceUrl(null)
        .setAcceptHeader(null)
        .setChangedResourcesCount(null)
        .setRemovedResourcesCount(null)
        .build()

    private fun sendHarvestEvent(
        harvestRunId: String?,
        fdkId: String,
        eventDescription: String,
        buildEvent: (String) -> HarvestEvent,
    ) {
        if (harvestRunId == null) {
            logger.debug("Skipping harvest event - harvestRunId is null for fdkId: $fdkId")
            return
        }

        try {
            kafkaTemplate.send(KafkaTopics.HARVEST_EVENTS, harvestRunId, buildEvent(harvestRunId))
            logger.debug("Produced $eventDescription for fdkId: $fdkId, harvestRunId: $harvestRunId")
        } catch (e: Exception) {
            logger.error("Error producing $eventDescription for fdkId: $fdkId", e)
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(HarvestEventProducer::class.java)
    }
}
