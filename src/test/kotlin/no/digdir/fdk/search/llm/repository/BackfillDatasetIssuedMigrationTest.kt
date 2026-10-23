package no.digdir.fdk.search.llm.repository

import com.pgvector.PGvector
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import kotlin.test.assertEquals

@Testcontainers(disabledWithoutDocker = true)
class BackfillDatasetIssuedMigrationTest {
    companion object {
        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer =
            PostgreSQLContainer("pgvector/pgvector:pg16")
                .withDatabaseName("llm-search")
                .withUsername("postgres")
                .withPassword("S3cret")
    }

    private fun flyway(dataSource: DriverManagerDataSource, target: String) = Flyway
        .configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .sqlMigrationSuffixes(".SQL")
        .cleanDisabled(false)
        .target(target)
        .load()

    @Test
    fun `backfills issued for datasets from the embedded summary`() {
        val dataSource =
            DriverManagerDataSource().apply {
                setDriverClassName(postgres.driverClassName)
                url = postgres.jdbcUrl
                username = postgres.username
                password = postgres.password
            }
        flyway(dataSource, "1.4").apply {
            clean()
            migrate()
        }

        val jdbcTemplate = NamedParameterJdbcTemplate(dataSource)
        val insert =
            """
            INSERT INTO embeddings (id, content, timestamp, deleted, metadata, embedding)
                VALUES (:id, :content, 1, false, :metadata::jsonb, :embedding)
            """.trimIndent()
        val vector = PGvector(FloatArray(768) { 0.1f })
        val dataset = """{"type":"DATASET"}"""
        listOf(
            Triple("dataset", "Datasettet ble utgitt 09.04.2026 og oppdateres årlig.", dataset),
            Triple("dataset-period", "Datasettet ble utgitt 12.06.2016.", dataset),
            Triple(
                "dataset-date-in-description",
                "Beskrivelse:\nDatasettet ble utgitt 01.01.2000 på papir.\n\nDatasettet ble utgitt 05.05.2021.",
                dataset,
            ),
            Triple("dataset-impossible-date", "Datasettet ble utgitt 31.02.2020.", dataset),
            Triple("dataset-out-of-range", "Datasettet ble utgitt 45.13.2020.", dataset),
            Triple("dataset-malformed-date", "Datasettet ble utgitt 1.2.2020.", dataset),
            Triple("dataset-no-date", "Datasettet oppdateres årlig.", dataset),
            Triple("dataset-null-content", null, dataset),
            Triple("dataset-issued", "Datasettet ble utgitt 01.01.2020.", """{"type":"DATASET","issued":"2021-05-05"}"""),
            Triple("service", "Datasettet ble utgitt 01.01.2020.", """{"type":"SERVICE"}"""),
        ).forEach { (id, content, metadata) ->
            jdbcTemplate.update(insert, mapOf("id" to id, "content" to content, "metadata" to metadata, "embedding" to vector))
        }

        flyway(dataSource, "latest").migrate()

        val issued =
            jdbcTemplate
                .query("SELECT id, metadata->>'issued' AS issued FROM embeddings", emptyMap<String, Any>()) { rs, _ ->
                    rs.getString("id") to rs.getString("issued")
                }.toMap()
        assertEquals(
            mapOf(
                "dataset" to "2026-04-09",
                "dataset-period" to "2016-06-12",
                "dataset-date-in-description" to "2021-05-05",
                "dataset-impossible-date" to null,
                "dataset-out-of-range" to null,
                "dataset-malformed-date" to null,
                "dataset-no-date" to null,
                "dataset-null-content" to null,
                "dataset-issued" to "2021-05-05",
                "service" to null,
            ),
            issued,
        )
    }
}
