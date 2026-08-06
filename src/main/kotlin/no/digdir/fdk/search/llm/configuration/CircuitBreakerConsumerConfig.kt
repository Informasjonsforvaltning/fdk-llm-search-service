package no.digdir.fdk.search.llm.configuration

import io.github.resilience4j.circuitbreaker.CircuitBreaker.StateTransition
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent
import no.digdir.fdk.search.llm.kafka.KafkaManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration

@Configuration
class CircuitBreakerConsumerConfig(
    private val circuitBreakerRegistry: CircuitBreakerRegistry,
    private val kafkaManager: KafkaManager
) {

    init {
        logger.debug("Configuring circuit breaker event listener")
        circuitBreakerRegistry.circuitBreaker(CircuitBreakerNames.RDF_PARSE).eventPublisher.onStateTransition { event: CircuitBreakerOnStateTransitionEvent ->
            when (event.stateTransition) {
                StateTransition.CLOSED_TO_OPEN,
                StateTransition.CLOSED_TO_FORCED_OPEN,
                StateTransition.HALF_OPEN_TO_OPEN -> kafkaManager.pause(CircuitBreakerNames.RDF_PARSE)

                StateTransition.OPEN_TO_HALF_OPEN,
                StateTransition.HALF_OPEN_TO_CLOSED,
                StateTransition.FORCED_OPEN_TO_CLOSED,
                StateTransition.FORCED_OPEN_TO_HALF_OPEN -> kafkaManager.resume(CircuitBreakerNames.RDF_PARSE)

                else -> throw IllegalStateException("Unknown transition state: " + event.stateTransition)
            }
        }

        circuitBreakerRegistry.circuitBreaker(CircuitBreakerNames.REMOVE).eventPublisher.onStateTransition { event: CircuitBreakerOnStateTransitionEvent ->
            when (event.stateTransition) {
                StateTransition.CLOSED_TO_OPEN,
                StateTransition.CLOSED_TO_FORCED_OPEN,
                StateTransition.HALF_OPEN_TO_OPEN -> kafkaManager.pause(CircuitBreakerNames.REMOVE)

                StateTransition.OPEN_TO_HALF_OPEN,
                StateTransition.HALF_OPEN_TO_CLOSED,
                StateTransition.FORCED_OPEN_TO_CLOSED,
                StateTransition.FORCED_OPEN_TO_HALF_OPEN -> kafkaManager.resume(CircuitBreakerNames.REMOVE)

                else -> throw IllegalStateException("Unknown transition state: " + event.stateTransition)
            }
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(CircuitBreakerConsumerConfig::class.java)
    }
}
