package no.digdir.fdk.search.llm.kafka

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.micrometer.core.instrument.Metrics
import no.digdir.fdk.search.llm.service.EmbeddingService
import no.fdk.concept.ConceptEvent
import no.fdk.dataservice.DataServiceEvent
import no.fdk.dataset.DatasetEvent
import no.fdk.dataset.DatasetEventType
import no.fdk.event.EventEvent
import no.fdk.informationmodel.InformationModelEvent
import no.fdk.service.ServiceEvent
import org.apache.avro.specific.SpecificRecord
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
    private fun SpecificRecord.getResourceType(): String {
        return when (this) {
            is DatasetEvent -> "dataset"
            is DataServiceEvent -> "data-service"
            is ConceptEvent -> "concept"
            is InformationModelEvent -> "information-model"
            is ServiceEvent -> "service"
            is EventEvent -> "event"
            else -> "invalid-type"
        }
    }

    private fun SpecificRecord.getHarvestRunId(): String? {
        return when (this) {
            is DatasetEvent -> this.harvestRunId?.toString()
            is DataServiceEvent -> this.harvestRunId?.toString()
            is ConceptEvent -> this.harvestRunId?.toString()
            is InformationModelEvent -> this.harvestRunId?.toString()
            is ServiceEvent -> this.harvestRunId?.toString()
            is EventEvent -> this.harvestRunId?.toString()
            else -> null
        }
    }

    private fun SpecificRecord.getUri(): String? {
        return when (this) {
            is DatasetEvent -> this.uri?.toString()
            is DataServiceEvent -> this.uri?.toString()
            is ConceptEvent -> this.uri?.toString()
            is InformationModelEvent -> this.uri?.toString()
            is ServiceEvent -> this.uri?.toString()
            is EventEvent -> this.uri?.toString()
            else -> null
        }
    }

    private fun SpecificRecord.getFdkId(): String? {
        return when (this) {
            is DatasetEvent -> this.fdkId?.toString()
            is DataServiceEvent -> this.fdkId?.toString()
            is ConceptEvent -> this.fdkId?.toString()
            is InformationModelEvent -> this.fdkId?.toString()
            is ServiceEvent -> this.fdkId?.toString()
            is EventEvent -> this.fdkId?.toString()
            else -> null
        }
    }

    @CircuitBreaker(name = "remove")
    @Transactional
    open fun process(record: ConsumerRecord<String, SpecificRecord>) {
        logger.debug("Received message - offset: " + record.offset())

        val event = record.value()
        val harvestRunId = event.getHarvestRunId()
        val uri = event.getUri()
        val fdkId = event.getFdkId() ?: return
        val resourceType = event.getResourceType()
        val startTime = Instant.now()
        
        try {
            val (deleted, timeElapsed) = measureTimedValue {
                when {
                    event is DatasetEvent && event.type == DatasetEventType.DATASET_REMOVED -> {
                        logger.debug("Remove embedding - id: {}", event.fdkId)
                        embeddingService.markDeletedByIdAndBeforeTimestamp(event.fdkId.toString(), event.timestamp)
                    }
                    event is DataServiceEvent && event.type == no.fdk.dataservice.DataServiceEventType.DATA_SERVICE_REMOVED -> {
                        logger.debug("Remove embedding - id: {}", event.fdkId)
                        embeddingService.markDeletedByIdAndBeforeTimestamp(event.fdkId.toString(), event.timestamp)
                    }
                    event is ConceptEvent && event.type == no.fdk.concept.ConceptEventType.CONCEPT_REMOVED -> {
                        logger.debug("Remove embedding - id: {}", event.fdkId)
                        embeddingService.markDeletedByIdAndBeforeTimestamp(event.fdkId.toString(), event.timestamp)
                    }
                    event is InformationModelEvent && event.type == no.fdk.informationmodel.InformationModelEventType.INFORMATION_MODEL_REMOVED -> {
                        logger.debug("Remove embedding - id: {}", event.fdkId)
                        embeddingService.markDeletedByIdAndBeforeTimestamp(event.fdkId.toString(), event.timestamp)
                    }
                    event is ServiceEvent && event.type == no.fdk.service.ServiceEventType.SERVICE_REMOVED -> {
                        logger.debug("Remove embedding - id: {}", event.fdkId)
                        embeddingService.markDeletedByIdAndBeforeTimestamp(event.fdkId.toString(), event.timestamp)
                    }
                    event is EventEvent && event.type == no.fdk.event.EventEventType.EVENT_REMOVED -> {
                        logger.debug("Remove embedding - id: {}", event.fdkId)
                        embeddingService.markDeletedByIdAndBeforeTimestamp(event.fdkId.toString(), event.timestamp)
                    }
                    else -> {
                        logger.debug("Unknown event type: {}, skipping", event)
                        false
                    }
                } ?: false
            }
            val endTime = Instant.now()

            if (deleted) {
                Metrics.timer("embedding_delete", "type", resourceType)
                    .record(timeElapsed.toJavaDuration())
                
                // Produce harvest event on successful deletion
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
        } catch (e: Exception) {
            val endTime = Instant.now()
            logger.error("Error processing message", e)
            Metrics.counter(
                "embedding_delete_error",
                "type", resourceType
            ).increment()
            
            // Produce harvest event on deletion failure
            try {
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
