package no.digdir.fdk.search.llm.service

import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.request.ChatRequest
import dev.langchain4j.model.chat.response.ChatResponse
import dev.langchain4j.model.vertexai.VertexAiEmbeddingModel
import dev.langchain4j.model.vertexai.VertexAiGeminiChatModel
import no.digdir.fdk.search.llm.configuration.AiProperties
import org.springframework.stereotype.Service


@Service
class VertexService(
    private val aiProperties: AiProperties
) {
    private val maxInputTokensForEmbedding = 2048

    private val chatModel: VertexAiGeminiChatModel = VertexAiGeminiChatModel.builder()
        .project(aiProperties.vertex?.project)
        .location(aiProperties.vertex?.location)
        .temperature(aiProperties.vertex?.temperature)
        .modelName("gemini-2.0-flash")
        .build()

    private val embeddingModel: VertexAiEmbeddingModel = VertexAiEmbeddingModel.builder()
        .endpoint(aiProperties.vertex?.endpoint)
        .project(aiProperties.vertex?.project)
        .location(aiProperties.vertex?.location)
        .publisher("google")
        .modelName("text-multilingual-embedding-002")
        .build()

    /**
     * Embed text and limit input tokens
     */
    fun embed(text: String?): Embedding {
        return embeddingModel.embed(text?.take(maxInputTokensForEmbedding)).content()
    }

    /**
     * Generate AI response using Chat model
     */
    fun chat(message: String): String {
        val userMessage = UserMessage.from(message)
        val chatRequest: ChatRequest? = ChatRequest.builder().messages(userMessage).build()

        val response: ChatResponse? = chatModel.chat(chatRequest)
        return response?.aiMessage()?.text().orEmpty()
    }
}
