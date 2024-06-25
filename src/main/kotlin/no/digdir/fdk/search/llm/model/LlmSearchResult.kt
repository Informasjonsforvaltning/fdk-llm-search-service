package no.digdir.fdk.search.llm.model

data class LlmSearchHit(
    val id : String,
    val title : String,
    val description : String,
    val type : String,
    val publisher : String,
    val publisherId: String,
)

data class LlmSearchResult(
    val hits : List<LlmSearchHit>
)
