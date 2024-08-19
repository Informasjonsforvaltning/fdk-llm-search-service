package no.digdir.fdk.search.llm.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("ai")
data class AiProperties(
    var vertex: VertexProperties? = null
)

data class VertexProperties(
    var endpoint: String? = null,
    var project: String? = null,
    var location: String? = null,
    var llmModelName: String? = null,
    var embeddingModelName: String? = null,
    var maxOutputTokens: Int? = null,
    var topK: Int? = null,
    var topP: Double? = null,
    var temperature: Double? = null
)
