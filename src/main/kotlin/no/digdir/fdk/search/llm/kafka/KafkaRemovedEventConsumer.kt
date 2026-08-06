package no.digdir.fdk.search.llm.kafka

import no.digdir.fdk.search.llm.configuration.CircuitBreakerNames
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class KafkaRemovedEventConsumer(
    private val kafkaRemovedEventCircuitBreaker: KafkaRemovedEventCircuitBreaker
) {

    @KafkaListener(
        topics = [
            KafkaTopics.DATASET_EVENTS,
            KafkaTopics.DATA_SERVICE_EVENTS,
            KafkaTopics.CONCEPT_EVENTS,
            KafkaTopics.INFORMATION_MODEL_EVENTS,
            KafkaTopics.EVENT_EVENTS,
            KafkaTopics.SERVICE_EVENTS],
        groupId = "fdk-llm-search-service",
        concurrency = "4",
        containerFactory = "kafkaListenerContainerFactory",
        id = CircuitBreakerNames.REMOVE
    )
    fun listen(record: ConsumerRecord<String, GenericRecord>, ack: Acknowledgment) {
        ack.ackOrNack {
            kafkaRemovedEventCircuitBreaker.process(record)
        }
    }
}
