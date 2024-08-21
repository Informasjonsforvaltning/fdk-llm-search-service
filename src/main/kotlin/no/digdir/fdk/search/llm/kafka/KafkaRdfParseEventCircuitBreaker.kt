package no.digdir.fdk.search.llm.kafka

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.micrometer.core.instrument.Metrics
import no.digdir.fdk.search.llm.model.Dataset
import no.digdir.fdk.search.llm.service.EmbeddingService
import no.fdk.rdf.parse.RdfParseEvent
import no.fdk.rdf.parse.RdfParseResourceType
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import kotlin.time.measureTimedValue
import kotlin.time.toJavaDuration

@Component
open class KafkaRdfParseEventCircuitBreaker(
    private val embeddingService: EmbeddingService
) {

    private fun storeEmbedding(
        event: RdfParseEvent
    ) { val dataset = jacksonObjectMapper().readValue(event.data.toString(), Dataset::class.java)
        embeddingService.storeDatasetEmbedding(event.fdkId.toString(), dataset, event.timestamp)
    }

    @CircuitBreaker(name = "rdf-parse")
    @Transactional
    open fun process(
        record: ConsumerRecord<String, RdfParseEvent>
    ) {
        logger.debug("CB Received message - offset: " + record.offset())

        val event = record.value()
        try {
            val timeElapsed = measureTimedValue {
                if (event?.resourceType == RdfParseResourceType.DATASET) {
                    logger.debug("Store embedding for dataset - id: " + event.fdkId)
                    storeEmbedding(event)
                }
            }
            Metrics.timer("store_embedding", "type", event.resourceType.name.lowercase())
                .record(timeElapsed.duration.toJavaDuration())
        } catch (e: Exception) {
            logger.error("Error processing message", e)
            Metrics.counter(
                "store_embedding_error",
                "type", event.resourceType.name.lowercase()
            ).increment()
            throw e
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(KafkaRdfParseEventCircuitBreaker::class.java)
    }
}
