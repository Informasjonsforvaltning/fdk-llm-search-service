package no.digdir.fdk.search.llm.configuration

import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import org.apache.avro.generic.GenericRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties

@EnableKafka
@Configuration
open class KafkaConsumerConfig {

    @Bean
    open fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, GenericRecord>
    ): ConcurrentKafkaListenerContainerFactory<String, GenericRecord> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, GenericRecord>()
        factory.consumerFactory = consumerFactory
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        return factory
    }

    @Bean
    open fun consumerFactory(
        @Value("\${spring.kafka.bootstrap-servers}") bootstrapServers: String,
        @Value("\${spring.kafka.properties.schema.registry.url}") schemaRegistryUrl: String
    ): ConsumerFactory<String, GenericRecord> {
        val props = mapOf(
            "bootstrap.servers" to bootstrapServers,
            "key.deserializer" to "org.apache.kafka.common.serialization.StringDeserializer",
            "value.deserializer" to "io.confluent.kafka.serializers.KafkaAvroDeserializer",
            "schema.registry.url" to schemaRegistryUrl,
            KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to false,
            "auto.offset.reset" to "earliest",
            "enable.auto.commit" to false,
            "auto.register.schemas" to false,
            "use.latest.version" to true,
            "value.subject.name.strategy" to "io.confluent.kafka.serializers.subject.RecordNameStrategy",
            "key.subject.name.strategy" to "io.confluent.kafka.serializers.subject.RecordNameStrategy"
        )
        return DefaultKafkaConsumerFactory(props)
    }
}
