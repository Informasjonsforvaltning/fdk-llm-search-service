package no.digdir.fdk.search.llm.configuration

import no.fdk.rdf.parse.RdfParseEvent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.listener.ContainerProperties

@EnableKafka
@Configuration
open class KafkaConsumerConfig {

    @Bean
    open fun kafkaListenerContainerFactory(consumerFactory: ConsumerFactory<String, RdfParseEvent>): ConcurrentKafkaListenerContainerFactory<String, RdfParseEvent> {
        val factory: ConcurrentKafkaListenerContainerFactory<String, RdfParseEvent> =
            ConcurrentKafkaListenerContainerFactory()
        factory.consumerFactory = consumerFactory
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        return factory
    }
}
