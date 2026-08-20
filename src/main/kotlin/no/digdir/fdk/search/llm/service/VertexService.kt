package no.digdir.fdk.search.llm.service

import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.vertexai.VertexAiEmbeddingModel
import no.digdir.fdk.search.llm.configuration.AiProperties
import org.springframework.stereotype.Service

@Service
class VertexService(private val aiProperties: AiProperties) {
    private val maxInputTokensForEmbedding = 2048

    private val embeddingModel: EmbeddingModel =
        VertexAiEmbeddingModel
            .builder()
            .endpoint(aiProperties.vertex?.endpoint)
            .project(aiProperties.vertex?.project)
            .location(aiProperties.vertex?.location)
            .publisher("google")
            .modelName(aiProperties.vertex?.embeddingModelName)
            .build()

    /**
     * Embed text and limit input tokens
     */
    fun embed(text: String?): Embedding = embeddingModel.embed(text?.take(maxInputTokensForEmbedding)).content()
}
