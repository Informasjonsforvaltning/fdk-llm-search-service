package no.digdir.fdk.search.llm.kafka

import org.springframework.kafka.support.Acknowledgment
import java.time.Duration

fun Acknowledgment.ackOrNack(block: () -> Unit) {
    try {
        block()
        acknowledge()
    } catch (e: Exception) {
        nack(Duration.ZERO)
    }
}
