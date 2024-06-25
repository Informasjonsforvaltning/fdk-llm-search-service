package no.digdir.fdk.search.llm.kafka

import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class KafkaRemovedEventConsumer(
    private val kafkaRemovedEventCircuitBreaker: KafkaRemovedEventCircuitBreaker
) {

    @KafkaListener(
        topics = [
            "dataset-events",
            "data-service-events",
            "concept-events",
            "information-model-events",
            "event-events",
            "service-events"],
        groupId = "fdk-llm-search-service",
        concurrency = "4",
        containerFactory = "kafkaListenerContainerFactory",
        id = "remove"
    )
    fun listen(record: ConsumerRecord<String, SpecificRecord>, ack: Acknowledgment) {
        try {
            kafkaRemovedEventCircuitBreaker.process(record)
            ack.acknowledge()
        } catch (e: Exception) {
            ack.nack(Duration.ZERO)
        }
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(KafkaRemovedEventConsumer::class.java)
    }
}
