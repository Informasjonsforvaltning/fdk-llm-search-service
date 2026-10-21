package no.digdir.fdk.search.llm.configuration

import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.config.SaslConfigs
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import java.time.Duration

@EnableKafka
@Configuration
class KafkaConsumerConfig {
    @Bean
    fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, GenericRecord>,
    ): ConcurrentKafkaListenerContainerFactory<String, GenericRecord> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, GenericRecord>()
        factory.setConsumerFactory(consumerFactory)
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        // Keep retrying instead of stopping the listeners for good on an auth failure.
        factory.containerProperties.setAuthExceptionRetryInterval(Duration.ofSeconds(30))
        return factory
    }

    @Bean
    fun consumerFactory(
        @Value("\${spring.kafka.bootstrap-servers}") bootstrapServers: String,
        @Value("\${spring.kafka.properties.security.protocol}") securityProtocol: String,
        @Value("\${spring.kafka.properties.sasl.mechanism}") saslMechanism: String,
        @Value("\${spring.kafka.properties.sasl.jaas.config}") saslJaasConfig: String,
        @Value("\${spring.kafka.properties.schema.registry.url}") schemaRegistryUrl: String,
    ): ConsumerFactory<String, GenericRecord> {
        val props =
            mapOf(
                "bootstrap.servers" to bootstrapServers,
                CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to securityProtocol,
                SaslConfigs.SASL_MECHANISM to saslMechanism,
                SaslConfigs.SASL_JAAS_CONFIG to saslJaasConfig,
                "key.deserializer" to "org.apache.kafka.common.serialization.StringDeserializer",
                "value.deserializer" to "io.confluent.kafka.serializers.KafkaAvroDeserializer",
                "schema.registry.url" to schemaRegistryUrl,
                KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to false,
                "auto.offset.reset" to "earliest",
                "enable.auto.commit" to false,
                "auto.register.schemas" to false,
                "use.latest.version" to true,
                "value.subject.name.strategy" to "io.confluent.kafka.serializers.subject.RecordNameStrategy",
                "key.subject.name.strategy" to "io.confluent.kafka.serializers.subject.RecordNameStrategy",
            )
        return DefaultKafkaConsumerFactory(props)
    }
}
