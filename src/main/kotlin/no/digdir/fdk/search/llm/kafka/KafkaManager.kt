package no.digdir.fdk.search.llm.kafka

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.stereotype.Component

@Component
class KafkaManager(private val registry: KafkaListenerEndpointRegistry) {
    fun pause(id: String) {
        logger.debug("Pausing kafka listener containers with id: $id")
        registry.listenerContainers
            .filter { it.listenerId == id }
            .forEach { it.pause() }
    }

    fun resume(id: String) {
        logger.debug("Resuming kafka listener containers with id: $id")
        registry.listenerContainers
            .filter { it.listenerId == id }
            .forEach { it.resume() }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(KafkaManager::class.java)
    }
}
