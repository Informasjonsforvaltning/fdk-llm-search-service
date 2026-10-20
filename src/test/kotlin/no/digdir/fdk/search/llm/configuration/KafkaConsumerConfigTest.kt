package no.digdir.fdk.search.llm.configuration

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.config.SaslConfigs
import org.junit.jupiter.api.Test
import org.springframework.kafka.listener.ContainerProperties
import java.time.Duration
import kotlin.test.assertEquals

class KafkaConsumerConfigTest {
    private val config = KafkaConsumerConfig()

    private val consumerFactory =
        config.consumerFactory(
            bootstrapServers = "localhost:9092",
            securityProtocol = "SASL_PLAINTEXT",
            saslMechanism = "SCRAM-SHA-512",
            saslJaasConfig = "jaas-config",
            schemaRegistryUrl = "http://localhost:8081",
        )

    @Test
    fun consumerFactoryCarriesSecuritySettings() {
        val props = consumerFactory.configurationProperties

        assertEquals("SASL_PLAINTEXT", props[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG])
        assertEquals("SCRAM-SHA-512", props[SaslConfigs.SASL_MECHANISM])
        assertEquals("jaas-config", props[SaslConfigs.SASL_JAAS_CONFIG])
    }

    @Test
    fun listenerContainerFactoryRetriesOnAuthFailure() {
        val containerProperties = config.kafkaListenerContainerFactory(consumerFactory).containerProperties

        assertEquals(ContainerProperties.AckMode.MANUAL_IMMEDIATE, containerProperties.ackMode)
        assertEquals(Duration.ofSeconds(30), containerProperties.authExceptionRetryInterval)
    }
}
