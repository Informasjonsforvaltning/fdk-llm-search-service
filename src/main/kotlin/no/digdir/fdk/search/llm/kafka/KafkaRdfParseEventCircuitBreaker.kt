package no.digdir.fdk.search.llm.kafka

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.micrometer.core.instrument.Metrics
import no.digdir.fdk.search.llm.model.*
import no.digdir.fdk.search.llm.service.EmbeddingService
import no.fdk.rdf.parse.RdfParseResourceType
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
open class KafkaRdfParseEventCircuitBreaker(
    private val embeddingService: EmbeddingService,
    private val harvestEventProducer: HarvestEventProducer
) {

    private fun resourceTypeFromRecord(record: GenericRecord): RdfParseResourceType {
        val typeStr = (record.get("resourceType") ?: "").toString()
        return when (typeStr) {
            "DATASET" -> RdfParseResourceType.DATASET
            "DATA_SERVICE" -> RdfParseResourceType.DATA_SERVICE
            "CONCEPT" -> RdfParseResourceType.CONCEPT
            "INFORMATION_MODEL" -> RdfParseResourceType.INFORMATION_MODEL
            "SERVICE" -> RdfParseResourceType.SERVICE
            "EVENT" -> RdfParseResourceType.EVENT
            else -> throw IllegalArgumentException("Unknown resourceType: $typeStr")
        }
    }

    private fun storeEmbedding(record: GenericRecord, resourceType: RdfParseResourceType) {
        val mapper = jacksonObjectMapper()
        val fdkId = (record.get("fdkId") ?: "").toString()
        val data = (record.get("data") ?: "").toString()
        val timestamp = runCatching { (record.get("timestamp") as? Number)?.toLong() }.getOrNull() ?: 0L

        when (resourceType) {
            RdfParseResourceType.DATASET -> {
                val dataset = mapper.readValue(data, Dataset::class.java)
                embeddingService.storeDatasetEmbedding(fdkId, dataset, timestamp)
            }
            RdfParseResourceType.CONCEPT -> {
                val concept = mapper.readValue(data, Concept::class.java)
                embeddingService.storeConceptEmbedding(fdkId, concept, timestamp)
            }
            RdfParseResourceType.DATA_SERVICE -> {
                val dataService = mapper.readValue(data, DataService::class.java)
                embeddingService.storeDataServiceEmbedding(fdkId, dataService, timestamp)
            }
            RdfParseResourceType.INFORMATION_MODEL -> {
                val informationModel = mapper.readValue(data, InformationModel::class.java)
                embeddingService.storeInformationModelEmbedding(fdkId, informationModel, timestamp)
            }
            RdfParseResourceType.SERVICE -> {
                val serviceModel = mapper.readValue(data, no.digdir.fdk.search.llm.model.Service::class.java)
                embeddingService.storeServiceEmbedding(fdkId, serviceModel, timestamp)
            }
            RdfParseResourceType.EVENT -> {
                val eventModel = mapper.readValue(data, Event::class.java)
                embeddingService.storeEventEmbedding(fdkId, eventModel, timestamp)
            }
        }
    }

    @CircuitBreaker(name = "rdf-parse")
    @Transactional
    open fun process(
        record: ConsumerRecord<String, GenericRecord>
    ) {
        logger.debug("CB Received message - offset: " + record.offset())

        val event = record.value()
        val resourceType = resourceTypeFromRecord(event)
        val harvestRunId = runCatching { event.get("harvestRunId")?.toString() }.getOrNull()
        val uri = runCatching { event.get("uri")?.toString() }.getOrNull()
        val fdkId = (event.get("fdkId") ?: "").toString()
        val startTime = Instant.now()

        try {
            val timeElapsed = measureTimedValue {
                logger.debug("Store embedding for ${resourceType.name.lowercase()} - id: $fdkId")
                storeEmbedding(event, resourceType)
            }
            val endTime = Instant.now()

            Metrics.timer("store_embedding", "type", resourceType.name.lowercase())
                .record(timeElapsed.duration.toJavaDuration())

            harvestEventProducer.produceSuccessEvent(
                harvestRunId = harvestRunId,
                uri = uri,
                resourceType = resourceType,
                fdkId = fdkId,
                startTime = startTime,
                endTime = endTime
            )
        } catch (e: Exception) {
            val endTime = Instant.now()
            logger.error("Error processing message", e)
            Metrics.counter(
                "store_embedding_error",
                "type", resourceType.name.lowercase()
            ).increment()

            harvestEventProducer.produceFailureEvent(
                harvestRunId = harvestRunId,
                uri = uri,
                resourceType = resourceType,
                fdkId = fdkId,
                startTime = startTime,
                endTime = endTime,
                errorMessage = e.message ?: "Unknown error"
            )

            throw e
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(KafkaRdfParseEventCircuitBreaker::class.java)
    }
}
