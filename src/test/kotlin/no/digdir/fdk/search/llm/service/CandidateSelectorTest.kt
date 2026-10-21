package no.digdir.fdk.search.llm.service

import no.digdir.fdk.search.llm.model.SearchType
import no.digdir.fdk.search.llm.model.TextEmbedding
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CandidateSelectorTest {
    private fun candidate(title: String, publisherId: String = "lnd", type: SearchType = SearchType.DATASET, issued: String? = null) =
        TextEmbedding(
            id = title,
            content = "content",
            metadata =
            buildMap {
                put("title", title)
                put("type", type.name)
                put("publisherId", publisherId)
                if (issued != null) put("issued", issued)
            },
        )

    private fun titles(embeddings: List<TextEmbedding>) = embeddings.map { it.metadata?.get("title") }

    // Similarity order from prod for "Datasett som inneholder Produksjons- og avløsertilskudd til jordbruksforetak" (issue #133)
    private val seriesBySimilarity =
        listOf(
            candidate("Produksjons- og avløsertilskudd jordbruksforetak - søknadsomgang januar 2014"),
            candidate("Produksjons- og avløsertilskudd jordbruksforetak - søknadsomgang januar 2015"),
            candidate("Produksjons- og avløsertilskudd jordbruksforetak - søknadsomgang januar 2017"),
            candidate("Produksjons- og avløsertilskudd jordbruksforetak - søknadsomgang januar 2016"),
            candidate("Produksjons- og avløsertilskudd til jordbruksforetak – søknadsomgang 2021"),
            candidate("Produksjons- og avløsertilskudd til jordbruksforetak  søknadsomgang 2017"),
            candidate("Produksjons- og avløsertilskudd til jordbruksforetak  søknadsomgang 2018"),
            candidate("Produksjonstilskudd jordbruksforetak - søknadsomgang august 2016"),
            candidate("Produksjons- og avløsertilskudd til jordbruksforetak – søknadsomgang 2022"),
            candidate("Produksjons- og avløsertilskudd til jordbruksforetak – søknadsomgang 2020"),
            candidate("Produksjons- og avløsertilskudd til jordbruksforetak – søknadsomgang 2023"),
            candidate("Produksjons- og avløsertilskudd til jordbruksforetak  søknadsomgang 2019"),
            candidate("Produksjons- og avløsertilskudd til jordbruksforetak – søknadsår 2025"),
            candidate("Produksjons- og avløsertilskudd til jordbruksforetak – søknadsår 2024"),
            candidate("Produksjons- og avløsertilskudd til jordbruksforetak – søknadsår 2026 del 1"),
        )

    @Test
    fun `keeps the newest member of each series when the query has no year`() {
        val selected =
            CandidateSelector.select("Datasett som inneholder Produksjons- og avløsertilskudd", seriesBySimilarity, 10, 1)

        assertEquals(
            listOf(
                "Produksjons- og avløsertilskudd jordbruksforetak - søknadsomgang januar 2017",
                "Produksjons- og avløsertilskudd til jordbruksforetak – søknadsomgang 2023",
                "Produksjonstilskudd jordbruksforetak - søknadsomgang august 2016",
                "Produksjons- og avløsertilskudd til jordbruksforetak – søknadsår 2026 del 1",
            ),
            titles(selected),
        )
    }

    @Test
    fun `prefers the series member matching a year in the query`() {
        val selected = CandidateSelector.select("Produksjons- og avløsertilskudd 2018", seriesBySimilarity, 10, 1)

        assertEquals(
            listOf(
                "Produksjons- og avløsertilskudd jordbruksforetak - søknadsomgang januar 2017",
                "Produksjons- og avløsertilskudd til jordbruksforetak  søknadsomgang 2018",
                "Produksjonstilskudd jordbruksforetak - søknadsomgang august 2016",
                "Produksjons- og avløsertilskudd til jordbruksforetak – søknadsår 2026 del 1",
            ),
            titles(selected),
        )
    }

    @Test
    fun `keeps every member matching a year in the query in addition to maxPerSeries`() {
        val selected = CandidateSelector.select("søknadsomgang 2019 og 2020", seriesBySimilarity, 10, 1)

        assertEquals(
            listOf(
                "Produksjons- og avløsertilskudd til jordbruksforetak – søknadsomgang 2020",
                "Produksjons- og avløsertilskudd til jordbruksforetak  søknadsomgang 2019",
            ),
            titles(selected).filter { it!!.contains("til jordbruksforetak") && it.contains("søknadsomgang") },
        )
    }

    @Test
    fun `keeps up to maxPerSeries newest members per series`() {
        val selected = CandidateSelector.select("Produksjons- og avløsertilskudd", seriesBySimilarity, 10, 2)

        assertEquals(
            listOf(
                "Produksjons- og avløsertilskudd jordbruksforetak - søknadsomgang januar 2017",
                "Produksjons- og avløsertilskudd jordbruksforetak - søknadsomgang januar 2016",
                "Produksjons- og avløsertilskudd til jordbruksforetak – søknadsomgang 2023",
                "Produksjons- og avløsertilskudd til jordbruksforetak – søknadsomgang 2022",
                "Produksjonstilskudd jordbruksforetak - søknadsomgang august 2016",
                "Produksjons- og avløsertilskudd til jordbruksforetak – søknadsår 2026 del 1",
                "Produksjons- og avløsertilskudd til jordbruksforetak – søknadsår 2025",
            ),
            titles(selected),
        )
    }

    @Test
    fun `does not merge resources from different publishers or types`() {
        val candidates =
            listOf(
                candidate("Strømstøtte 2024", publisherId = "a"),
                candidate("Strømstøtte 2025", publisherId = "b"),
                candidate("Strømstøtte 2023", publisherId = "a", type = SearchType.SERVICE),
            )

        assertEquals(titles(candidates), titles(CandidateSelector.select("strømstøtte", candidates, 10, 1)))
    }

    @Test
    fun `collapses duplicates without year by newest issued`() {
        val candidates =
            listOf(
                candidate("Strømstøtte for jordbruksforetak", issued = "2023-01-01"),
                candidate("Strømstøtte for jordbruksforetak", issued = "2024-06-01"),
                candidate("Pristilskudd"),
            )

        val selected = CandidateSelector.select("strømstøtte", candidates, 10, 1)

        assertEquals(listOf("2024-06-01", null), selected.map { it.metadata?.get("issued") })
    }

    @Test
    fun `keeps resources without title and applies the limit`() {
        val candidates = (1..5).map { TextEmbedding(id = "id$it", metadata = mapOf("type" to SearchType.DATASET.name)) }

        assertEquals(listOf("id1", "id2", "id3"), CandidateSelector.select("noe", candidates, 3, 1).map { it.id })
    }
}
