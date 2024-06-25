package no.digdir.fdk.search.llm.configuration

import io.github.resilience4j.circuitbreaker.CircuitBreaker.StateTransition
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent
import no.digdir.fdk.search.llm.kafka.KafkaManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration

@Configuration
open class CircuitBreakerConsumerConfig(
    private val circuitBreakerRegistry: CircuitBreakerRegistry,
    private val kafkaManager: KafkaManager
) {

    init {
        logger.debug("Configuring circuit breaker event listener")
        circuitBreakerRegistry.circuitBreaker("rdf-parse").eventPublisher.onStateTransition { event: CircuitBreakerOnStateTransitionEvent ->
            when (event.stateTransition) {
                StateTransition.CLOSED_TO_OPEN,
                StateTransition.CLOSED_TO_FORCED_OPEN,
                StateTransition.HALF_OPEN_TO_OPEN -> kafkaManager.pause("rdf-parse")

                StateTransition.OPEN_TO_HALF_OPEN,
                StateTransition.HALF_OPEN_TO_CLOSED,
                StateTransition.FORCED_OPEN_TO_CLOSED,
                StateTransition.FORCED_OPEN_TO_HALF_OPEN -> kafkaManager.resume("rdf-parse")

                else -> throw IllegalStateException("Unknown transition state: " + event.stateTransition)
            }
        }

        circuitBreakerRegistry.circuitBreaker("remove").eventPublisher.onStateTransition { event: CircuitBreakerOnStateTransitionEvent ->
            when (event.stateTransition) {
                StateTransition.CLOSED_TO_OPEN,
                StateTransition.CLOSED_TO_FORCED_OPEN,
                StateTransition.HALF_OPEN_TO_OPEN -> kafkaManager.pause("remove")

                StateTransition.OPEN_TO_HALF_OPEN,
                StateTransition.HALF_OPEN_TO_CLOSED,
                StateTransition.FORCED_OPEN_TO_CLOSED,
                StateTransition.FORCED_OPEN_TO_HALF_OPEN -> kafkaManager.resume("remove")

                else -> throw IllegalStateException("Unknown transition state: " + event.stateTransition)
            }
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(CircuitBreakerConsumerConfig::class.java)
    }
}
