package no.digdir.fdk.search.llm.controller

import dev.langchain4j.model.chat.ChatModel
import no.digdir.fdk.search.llm.service.SearchAssistant
import no.digdir.fdk.search.llm.service.VertexService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Verifies response compression through the real embedded server.
 * Uses the OpenAPI document, which is served without authentication and is well above the 1 KB threshold.
 *
 * Boots the full context against a pgvector Postgres container. Kafka listeners are kept stopped (no broker),
 * and the Vertex AI backed beans are mocked because they build GCP clients when constructed.
 */
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.kafka.listener.auto-startup=false",
        "application.cors.originPatterns=*",
        "ai.vertex.endpoint=localhost:443",
        "ai.vertex.project=test",
        "ai.vertex.location=test",
    ],
)
class CompressionTest {
    companion object {
        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer =
            PostgreSQLContainer("pgvector/pgvector:pg16")
                .withDatabaseName("llm-search")
                .withUsername("postgres")
                .withPassword("S3cret")

        @JvmStatic
        @DynamicPropertySource
        fun datasourceProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
        }
    }

    @MockitoBean
    private lateinit var chatModel: ChatModel

    @MockitoBean
    private lateinit var searchAssistant: SearchAssistant

    @MockitoBean
    private lateinit var vertexService: VertexService

    @LocalServerPort
    private var port = 0

    // java.net.http never decompresses on its own, so Content-Encoding and the body are seen as sent
    private val client = HttpClient.newHttpClient()

    private fun get(acceptEncoding: String?): HttpResponse<ByteArray> {
        val builder = HttpRequest.newBuilder(URI("http://localhost:$port/v3/api-docs")).GET()
        if (acceptEncoding != null) builder.header("Accept-Encoding", acceptEncoding)
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray())
    }

    @Test
    fun `gzips the response when the client accepts it`() {
        val response = get("gzip")
        assertEquals(200, response.statusCode())
        assertEquals("gzip", response.headers().firstValue("Content-Encoding").orElse(null))
        assertTrue(response.headers().allValues("Vary").joinToString(",").lowercase().contains("accept-encoding"))
        val body = response.body()
        assertTrue(body.size > 2 && body[0] == 0x1f.toByte() && body[1] == 0x8b.toByte(), "body is not gzip")
    }

    @Test
    fun `leaves the response uncompressed when the client does not ask`() {
        val response = get(null)
        assertEquals(200, response.statusCode())
        assertTrue(response.headers().firstValue("Content-Encoding").isEmpty)
        assertTrue(response.body().size > 1024, "test endpoint must exceed the 1 KB threshold")
    }
}
