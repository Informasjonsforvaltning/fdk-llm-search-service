package no.digdir.fdk.search.llm.service

import no.digdir.fdk.search.llm.model.TextEmbedding

/**
 * Narrows similarity search candidates before they are sent to the LLM.
 *
 * Resources published as a series (e.g. one dataset per year) have near-identical embeddings, so
 * arbitrary, often old, members of one series can fill every candidate slot. Candidates are grouped
 * into series by type, publisher and title with years, "del n" and month names removed. Within a
 * series, members whose title contains a year from the query come first, then the newest by title
 * year and `issued`. Each series keeps its year matches plus up to `maxPerSeries` members, and series
 * keep the order of their most similar member. Groups without any year in their titles are duplicates
 * rather than editions, so only one of them is kept.
 */
object CandidateSelector {
    private val yearRegex = Regex("""\b(19|20)\d{2}\b""")
    private val seriesNoiseRegex =
        Regex("""\bdel\s*\d+\b|\b(januar|februar|mars|april|mai|juni|juli|august|september|oktober|november|desember)\b""")
    private val nonWordRegex = Regex("""[^\p{L}\p{N}]+""")

    fun select(query: String, candidates: List<TextEmbedding>, limit: Int, maxPerSeries: Int): List<TextEmbedding> {
        val queryYears = years(query)
        val newestFirst =
            compareByDescending<TextEmbedding> { matchesYear(it, queryYears) }
                .thenByDescending { titleYears(it).maxOrNull() }
                .thenByDescending { it.metadata?.get("issued") }

        return candidates
            .groupBy(::seriesKey)
            .values
            .flatMap { series ->
                // Without years in the titles the members are duplicates, not editions of a series
                val keep = if (series.any { titleYears(it).isNotEmpty() }) maxPerSeries else 1
                series
                    .sortedWith(newestFirst)
                    .filterIndexed { index, member -> index < keep || matchesYear(member, queryYears) }
            }.take(limit)
    }

    private fun seriesKey(embedding: TextEmbedding): String {
        val title = embedding.metadata?.get("title")
        val normalized =
            title
                ?.lowercase()
                ?.replace(yearRegex, " ")
                ?.replace(seriesNoiseRegex, " ")
                ?.replace(nonWordRegex, " ")
                ?.trim()
        if (normalized.isNullOrEmpty()) return "id:${embedding.id}"

        return listOf(embedding.metadata["type"], embedding.metadata["publisherId"], normalized).joinToString("|")
    }

    private fun matchesYear(embedding: TextEmbedding, queryYears: Set<Int>): Boolean =
        queryYears.isNotEmpty() && titleYears(embedding).any { it in queryYears }

    private fun titleYears(embedding: TextEmbedding): Set<Int> = years(embedding.metadata?.get("title") ?: "")

    private fun years(text: String): Set<Int> = yearRegex.findAll(text).map { it.value.toInt() }.toSet()
}
