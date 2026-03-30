package no.digdir.fdk.search.llm.configuration

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration(proxyBeanMethods = false)
class CircuitBreakerRegistryConfig {

    @Bean
    fun circuitBreakerRegistry(): CircuitBreakerRegistry {
        val config = CircuitBreakerConfig.custom()
            .slidingWindowType(SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(10)
            .failureRateThreshold(50f)
            .permittedNumberOfCallsInHalfOpenState(3)
            .waitDurationInOpenState(Duration.ofSeconds(60))
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .build()

        val registry = CircuitBreakerRegistry.of(config)
        registry.circuitBreaker("rdf-parse")
        registry.circuitBreaker("remove")
        return registry
    }
}
