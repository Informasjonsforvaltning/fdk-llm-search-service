package no.digdir.fdk.search.llm.kafka

import no.digdir.fdk.search.llm.configuration.CircuitBreakerNames
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class KafkaRdfParseEventConsumer(private val kafkaRdfParseEventCircuitBreaker: KafkaRdfParseEventCircuitBreaker) {
    @KafkaListener(
        topics = [KafkaTopics.RDF_PARSE_EVENTS],
        groupId = "fdk-llm-search-service",
        containerFactory = "kafkaListenerContainerFactory",
        concurrency = "4",
        id = CircuitBreakerNames.RDF_PARSE,
    )
    fun listen(record: ConsumerRecord<String, GenericRecord>, ack: Acknowledgment) {
        ack.ackOrNack {
            kafkaRdfParseEventCircuitBreaker.process(record)
        }
    }
}
