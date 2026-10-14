package no.digdir.fdk.search.llm.repository

import com.pgvector.PGvector
import no.digdir.fdk.search.llm.model.SearchType
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Testcontainers(disabledWithoutDocker = true)
class EmbeddingRepositoryTest {
    companion object {
        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer =
            PostgreSQLContainer("pgvector/pgvector:pg16")
                .withDatabaseName("llm-search")
                .withUsername("postgres")
                .withPassword("S3cret")

        private val vector = FloatArray(768) { 0.1f }
        private val metadata = mapOf("type" to SearchType.DATASET.name, "title" to "Test")
    }

    private lateinit var repository: EmbeddingRepository
    private lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @BeforeEach
    fun setUp() {
        val dataSource =
            DriverManagerDataSource().apply {
                setDriverClassName(postgres.driverClassName)
                url = postgres.jdbcUrl
                username = postgres.username
                password = postgres.password
            }

        Flyway
            .configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .sqlMigrationSuffixes(".SQL")
            .cleanDisabled(false)
            .load()
            .apply {
                clean()
                migrate()
            }

        jdbcTemplate = NamedParameterJdbcTemplate(dataSource)
        repository = EmbeddingRepository(jdbcTemplate)
    }

    @Test
    fun `newer remove vs older save - remove wins and deleted is true`() {
        val id = "remove-wins"
        assertTrue(repository.saveEmbedding(id, "old content", vector, 1_000L, metadata))

        assertTrue(repository.markDeletedByIdAndBeforeTimestamp(id, 2_000L))
        assertFalse(repository.saveEmbedding(id, "stale content", vector, 1_500L, metadata))

        val row = repository.findById(id)
        assertNotNull(row)
        assertTrue(row.deleted)
        assertEquals(2_000L, row.timestamp)
        assertEquals("old content", row.content)
    }

    @Test
    fun `newer save vs older remove - save wins and deleted is false`() {
        val id = "save-wins"
        assertTrue(repository.markDeletedByIdAndBeforeTimestamp(id, 1_000L))

        assertTrue(repository.saveEmbedding(id, "fresh content", vector, 2_000L, metadata))
        assertFalse(repository.markDeletedByIdAndBeforeTimestamp(id, 1_500L))

        val row = repository.findById(id)
        assertNotNull(row)
        assertFalse(row.deleted)
        assertEquals(2_000L, row.timestamp)
        assertEquals("fresh content", row.content)
    }

    @Test
    fun `equal timestamps - no change and return false`() {
        val id = "equal-ts"
        assertTrue(repository.saveEmbedding(id, "original", vector, 1_000L, metadata))

        assertFalse(repository.saveEmbedding(id, "ignored save", vector, 1_000L, metadata))
        assertFalse(repository.markDeletedByIdAndBeforeTimestamp(id, 1_000L))

        val row = repository.findById(id)
        assertNotNull(row)
        assertFalse(row.deleted)
        assertEquals(1_000L, row.timestamp)
        assertEquals("original", row.content)
    }

    @Test
    fun `delete-only insert then newer save - undelete and set content`() {
        val id = "tombstone-then-save"
        assertTrue(repository.markDeletedByIdAndBeforeTimestamp(id, 1_000L))

        val tombstone = repository.findById(id)
        assertNotNull(tombstone)
        assertTrue(tombstone.deleted)
        assertNull(tombstone.content)

        assertTrue(repository.saveEmbedding(id, "restored content", vector, 2_000L, metadata))

        val row = repository.findById(id)
        assertNotNull(row)
        assertFalse(row.deleted)
        assertEquals(2_000L, row.timestamp)
        assertEquals("restored content", row.content)
    }

    @Test
    fun `concurrent same-id upserts - newer timestamp wins`() {
        val id = "concurrent-upsert"
        val older = 1_000L
        val newer = 2_000L
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)

        try {
            val olderSave =
                executor.submit<Boolean> {
                    start.await()
                    repository.saveEmbedding(id, "older content", vector, older, metadata)
                }
            val newerDelete =
                executor.submit<Boolean> {
                    start.await()
                    repository.markDeletedByIdAndBeforeTimestamp(id, newer)
                }

            start.countDown()
            olderSave.get(10, TimeUnit.SECONDS)
            newerDelete.get(10, TimeUnit.SECONDS)

            val row = repository.findById(id)
            assertNotNull(row)
            assertTrue(row.deleted)
            assertEquals(newer, row.timestamp)
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `null stored timestamp - precheck and save agree that incoming zero is newer`() {
        val id = "null-timestamp"
        insertRowWithNullTimestamp(id)

        assertTrue(repository.shouldProcessMessage(id, 0L))
        assertTrue(repository.saveEmbedding(id, "backfilled", vector, 0L, metadata))

        val row = repository.findById(id)
        assertNotNull(row)
        assertFalse(row.deleted)
        assertEquals(0L, row.timestamp)
        assertEquals("backfilled", row.content)
    }

    @Test
    fun `null stored timestamp - precheck and delete agree that incoming zero is newer`() {
        val id = "null-timestamp-delete"
        insertRowWithNullTimestamp(id)

        assertTrue(repository.shouldProcessMessage(id, 0L))
        assertTrue(repository.markDeletedByIdAndBeforeTimestamp(id, 0L))

        val row = repository.findById(id)
        assertNotNull(row)
        assertTrue(row.deleted)
        assertEquals(0L, row.timestamp)
    }

    @Test
    fun `concurrent same-id saves - newer timestamp content wins`() {
        val id = "concurrent-saves"
        val older = 1_000L
        val newer = 2_000L
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)

        try {
            val olderSave =
                executor.submit<Boolean> {
                    start.await()
                    repository.saveEmbedding(id, "older content", vector, older, metadata)
                }
            val newerSave =
                executor.submit<Boolean> {
                    start.await()
                    repository.saveEmbedding(id, "newer content", vector, newer, metadata)
                }

            start.countDown()
            olderSave.get(10, TimeUnit.SECONDS)
            newerSave.get(10, TimeUnit.SECONDS)

            val row = repository.findById(id)
            assertNotNull(row)
            assertFalse(row.deleted)
            assertEquals(newer, row.timestamp)
            assertEquals("newer content", row.content)
        } finally {
            executor.shutdownNow()
        }
    }

    private fun insertRowWithNullTimestamp(id: String) {
        jdbcTemplate.update(
            """
            INSERT INTO embeddings (id, content, timestamp, deleted, metadata, embedding)
            VALUES (
                :id,
                'legacy',
                NULL,
                false,
                :metadata::jsonb,
                :embedding
            )
            """.trimIndent(),
            mapOf(
                "id" to id,
                "metadata" to """{"type":"DATASET","title":"Test"}""",
                "embedding" to PGvector(vector),
            ),
        )
        assertNull(repository.findById(id)?.timestamp)
    }
}
