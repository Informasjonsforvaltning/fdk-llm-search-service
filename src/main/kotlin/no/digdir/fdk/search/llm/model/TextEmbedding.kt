package no.digdir.fdk.search.llm.model

data class TextEmbedding(
    val id: String? = null,
    val content: String? = null,
    val deleted: Boolean = false,
    val timestamp: Long = 0,
    val metadata: Map<String, String?>? = null
)
