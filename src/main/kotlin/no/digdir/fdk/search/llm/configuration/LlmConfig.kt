package no.digdir.fdk.search.llm.configuration

import dev.langchain4j.model.chat.Capability
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.vertexai.gemini.VertexAiGeminiChatModel
import dev.langchain4j.service.AiServices
import no.digdir.fdk.search.llm.service.SearchAssistant
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class LlmConfig {
    @Bean
    fun chatModel(aiProperties: AiProperties): ChatModel = VertexAiGeminiChatModel
        .builder()
        .project(aiProperties.vertex?.project)
        .location(aiProperties.vertex?.location)
        .temperature(aiProperties.vertex?.temperature)
        .maxOutputTokens(aiProperties.vertex?.maxOutputTokens)
        .topP(aiProperties.vertex?.topP)
        .modelName(aiProperties.vertex?.llmModelName)
        .responseMimeType("application/json")
        .supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA)
        .build()

    @Bean
    fun searchAssistant(chatModel: ChatModel): SearchAssistant = AiServices
        .builder(SearchAssistant::class.java)
        .chatModel(chatModel)
        .build()
}
