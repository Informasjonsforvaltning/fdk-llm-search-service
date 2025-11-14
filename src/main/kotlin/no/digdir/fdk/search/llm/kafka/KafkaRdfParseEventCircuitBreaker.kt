package no.digdir.fdk.search.llm.kafka

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.micrometer.core.instrument.Metrics
import no.digdir.fdk.search.llm.model.*
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

    private fun storeEmbedding(event: RdfParseEvent) {
        val mapper = jacksonObjectMapper()
        val fdkId = event.fdkId.toString()
        
        when (event.resourceType) {
            RdfParseResourceType.DATASET -> {
                val dataset = mapper.readValue(event.data.toString(), Dataset::class.java)
                embeddingService.storeDatasetEmbedding(fdkId, dataset, event.timestamp)
            }
            RdfParseResourceType.CONCEPT -> {
                val concept = mapper.readValue(event.data.toString(), Concept::class.java)
                embeddingService.storeConceptEmbedding(fdkId, concept, event.timestamp)
            }
            RdfParseResourceType.DATA_SERVICE -> {
                val dataService = mapper.readValue(event.data.toString(), DataService::class.java)
                embeddingService.storeDataServiceEmbedding(fdkId, dataService, event.timestamp)
            }
            RdfParseResourceType.INFORMATION_MODEL -> {
                val informationModel = mapper.readValue(event.data.toString(), InformationModel::class.java)
                embeddingService.storeInformationModelEmbedding(fdkId, informationModel, event.timestamp)
            }
            RdfParseResourceType.SERVICE -> {
                val serviceModel = mapper.readValue(event.data.toString(), no.digdir.fdk.search.llm.model.Service::class.java)
                embeddingService.storeServiceEmbedding(fdkId, serviceModel, event.timestamp)
            }
            RdfParseResourceType.EVENT -> {
                val eventModel = mapper.readValue(event.data.toString(), Event::class.java)
                embeddingService.storeEventEmbedding(fdkId, eventModel, event.timestamp)
            }
        }
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
                logger.debug("Store embedding for ${event.resourceType.name.lowercase()} - id: ${event.fdkId}")
                storeEmbedding(event)
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
