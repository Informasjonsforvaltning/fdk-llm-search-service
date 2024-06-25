package no.digdir.fdk.search.llm.kafka

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.stereotype.Component

@Component
class KafkaManager(
    private val registry: KafkaListenerEndpointRegistry
) {
    fun pause(id: String) {
        LOGGER.debug("Pausing kafka listener containers with id: $id")
        registry.listenerContainers
            .filter { it.listenerId.equals(id) }
            .forEach { it.pause() }
    }

    fun resume(id: String) {
        LOGGER.debug("Resuming kafka listener containers with id: $id")
        registry.listenerContainers
            .filter { it.listenerId.equals(id) }
            .forEach { it.resume() }
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(KafkaManager::class.java)
    }
}
