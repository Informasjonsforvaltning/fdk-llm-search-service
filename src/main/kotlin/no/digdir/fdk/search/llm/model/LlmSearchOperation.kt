package no.digdir.fdk.search.llm.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = "Search operation containing a natural language query to search the data catalog",
    example = """{"query": "Finn datasett om befolkning og demografi i Norge"}"""
)
data class LlmSearchOperation(
    @Schema(
        description = "Natural language search query in Norwegian. Must be between 3 and 255 characters.",
        example = "Finn datasett om befolkning og demografi i Norge",
        minLength = 3,
        maxLength = 255,
        required = true
    )
    val query : String
)
