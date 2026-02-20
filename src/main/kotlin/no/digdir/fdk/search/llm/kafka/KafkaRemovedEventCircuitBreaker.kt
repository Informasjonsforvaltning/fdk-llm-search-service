package no.digdir.fdk.search.llm.kafka

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.micrometer.core.instrument.Metrics
import no.digdir.fdk.search.llm.service.EmbeddingService
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import kotlin.time.measureTimedValue
import kotlin.time.toJavaDuration

@Component
open class KafkaRemovedEventCircuitBreaker(
    private val embeddingService: EmbeddingService,
    private val harvestEventProducer: HarvestEventProducer
) {

    private fun resourceTypeFromSchema(schemaName: String?): String = when (schemaName) {
        "no.fdk.dataset.DatasetEvent" -> "dataset"
        "no.fdk.dataservice.DataServiceEvent" -> "data-service"
        "no.fdk.concept.ConceptEvent" -> "concept"
        "no.fdk.informationmodel.InformationModelEvent" -> "information-model"
        "no.fdk.service.ServiceEvent" -> "service"
        "no.fdk.event.EventEvent" -> "event"
        else -> "invalid-type"
    }

    private fun isRemovedEvent(typeStr: String?): Boolean = when (typeStr) {
        "DATASET_REMOVED", "DATA_SERVICE_REMOVED", "CONCEPT_REMOVED",
        "INFORMATION_MODEL_REMOVED", "SERVICE_REMOVED", "EVENT_REMOVED" -> true
        else -> false
    }

    @CircuitBreaker(name = "remove")
    @Transactional
    open fun process(record: ConsumerRecord<String, GenericRecord>) {
        logger.debug("Received message - offset: " + record.offset())

        val event = record.value()
        val schemaName = event?.schema?.fullName
        val typeStr = event?.get("type")?.toString()
        val harvestRunId = runCatching { event?.get("harvestRunId")?.toString() }.getOrNull()
        val uri = runCatching { event?.get("uri")?.toString() }.getOrNull()
        val fdkId = event?.get("fdkId")?.toString() ?: return
        val timestamp = runCatching { (event?.get("timestamp") as? Number)?.toLong() }.getOrNull() ?: 0L
        val resourceType = resourceTypeFromSchema(schemaName)
        val startTime = Instant.now()

        try {
            val (deleted, timeElapsed) = measureTimedValue {
                if (isRemovedEvent(typeStr)) {
                    logger.debug("Remove embedding - id: {}", fdkId)
                    embeddingService.markDeletedByIdAndBeforeTimestamp(fdkId, timestamp)
                } else {
                    logger.debug("Unknown event type: {}, skipping", typeStr)
                    false
                }
            }
            val endTime = Instant.now()

            if (deleted) {
                Metrics.timer("embedding_delete", "type", resourceType)
                    .record(timeElapsed.toJavaDuration())

                if (resourceType != "invalid-type") {
                    val dataType = harvestEventProducer.mapResourceTypeStringToDataType(resourceType)
                    harvestEventProducer.produceDeletionSuccessEvent(
                    harvestRunId = harvestRunId,
                    uri = uri,
                    dataType = dataType,
                    fdkId = fdkId,
                    startTime = startTime,
                    endTime = endTime
                    )
                }
            }
        } catch (e: Exception) {
            val endTime = Instant.now()
            logger.error("Error processing message", e)
            Metrics.counter(
                "embedding_delete_error",
                "type", resourceType
            ).increment()
            
            // Produce harvest event on deletion failure
            if (resourceType != "invalid-type") try {
                val dataType = harvestEventProducer.mapResourceTypeStringToDataType(resourceType)
                harvestEventProducer.produceDeletionFailureEvent(
                    harvestRunId = harvestRunId,
                    uri = uri,
                    dataType = dataType,
                    fdkId = fdkId,
                    startTime = startTime,
                    endTime = endTime,
                    errorMessage = e.message ?: "Unknown error"
                )
            } catch (harvestEventError: Exception) {
                logger.error("Error producing harvest event for deletion failure", harvestEventError)
            }
            
            throw e
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(KafkaRemovedEventCircuitBreaker::class.java)
    }
}
