package no.digdir.fdk.search.llm.service

import dev.langchain4j.service.SystemMessage
import dev.langchain4j.service.UserMessage
import dev.langchain4j.service.V
import no.digdir.fdk.search.llm.model.AIResult

interface SearchAssistant {
    @SystemMessage(fromResource = "prompts/search-prompt.md")
    fun answer(
        @V("summaries") summaries: String,
        @UserMessage query: String,
    ): AIResult
}
