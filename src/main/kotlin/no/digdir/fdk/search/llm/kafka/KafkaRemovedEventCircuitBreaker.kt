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
import kotlin.time.measureTimedValue
import kotlin.time.toJavaDuration

@Component
open class KafkaRemovedEventCircuitBreaker(
    private val embeddingService: EmbeddingService
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

    @CircuitBreaker(name = "remove")
    @Transactional
    open fun process(record: ConsumerRecord<String, SpecificRecord>) {
        LOGGER.debug("Received message - offset: " + record.offset())

        val event = record.value()
        try {
            val (deleted, timeElapsed) = measureTimedValue {
                if (event is DatasetEvent && event.type == DatasetEventType.DATASET_REMOVED) {
                    LOGGER.debug("Remove embedding - id: {}", event.fdkId)
                    embeddingService.markDeletedByIdAndBeforeTimestamp(event.fdkId.toString(), event.timestamp)
                    true
                } else {
                    LOGGER.debug("Unknown event type: {}, skipping", event)
                    false
                }
            }

            if (deleted) {
                Metrics.timer("embedding_delete", "type", event.getResourceType())
                    .record(timeElapsed.toJavaDuration())
            }
        } catch (e: Exception) {
            LOGGER.error("Error processing message: " + e.message)
            Metrics.counter(
                "embedding_delete_error",
                "type", event.getResourceType()
            ).increment()
            throw e
        }
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(KafkaRemovedEventCircuitBreaker::class.java)
    }
}
