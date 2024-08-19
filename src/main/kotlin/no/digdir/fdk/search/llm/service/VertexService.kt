package no.digdir.fdk.search.llm.service

import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.model.input.Prompt
import dev.langchain4j.model.vertexai.VertexAiEmbeddingModel
import dev.langchain4j.model.vertexai.VertexAiLanguageModel
import no.digdir.fdk.search.llm.configuration.AiProperties
import org.springframework.stereotype.Service

@Service
class VertexService(
    private val aiProperties: AiProperties
) {
    private val maxInputTokensForEmbedding = 2048

    private val vertexAiLlm: VertexAiLanguageModel = VertexAiLanguageModel.builder()
        .endpoint(aiProperties.vertex?.endpoint)
        .project(aiProperties.vertex?.project)
        .location(aiProperties.vertex?.location)
        .publisher("google")
        .modelName(aiProperties.vertex?.llmModelName)
        .maxOutputTokens(aiProperties.vertex?.maxOutputTokens)
        .topK(aiProperties.vertex?.topK)
        .topP(aiProperties.vertex?.topP)
        .temperature(aiProperties.vertex?.temperature)
        .build()

    private val embeddingModel: VertexAiEmbeddingModel = VertexAiEmbeddingModel.builder()
        .endpoint(aiProperties.vertex?.endpoint)
        .project(aiProperties.vertex?.project)
        .location(aiProperties.vertex?.location)
        .publisher("google")
        .modelName(aiProperties.vertex?.embeddingModelName)
        .build()

    /**
     * Embed text and limit input tokens
     */
    fun embed(text: String?): Embedding {
        return embeddingModel.embed(text?.take(maxInputTokensForEmbedding)).content()
    }

    /**
     * Generate AI response using LLM
     */
    fun llmPrompt(prompt: Prompt): String {
        return vertexAiLlm.generate(prompt).content()
    }
}
