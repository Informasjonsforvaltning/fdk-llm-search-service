package no.digdir.fdk.search.llm.kafka

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

abstract class AbstractKafkaCircuitBreaker(
    circuitBreakerRegistry: CircuitBreakerRegistry,
    transactionManager: PlatformTransactionManager,
    circuitBreakerName: String,
) {
    private val circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName)
    private val transactionTemplate = TransactionTemplate(transactionManager)

    open fun process(record: ConsumerRecord<String, GenericRecord>) {
        circuitBreaker.executeRunnable {
            transactionTemplate.executeWithoutResult {
                processInTransaction(record)
            }
        }
    }

    protected abstract fun processInTransaction(record: ConsumerRecord<String, GenericRecord>)
}
