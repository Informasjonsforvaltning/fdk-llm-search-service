package no.digdir.fdk.search.llm.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = "Search operation containing a natural language query to search the data catalog",
    example = """{"query": "Finn datasett om befolkning og demografi i Norge", "type": "DATASET"}"""
)
data class LlmSearchOperation(
    @Schema(
        description = "Natural language search query in Norwegian. Must be between 3 and 255 characters.",
        example = "Finn datasett om befolkning og demografi i Norge",
        minLength = 3,
        maxLength = 255,
        required = true
    )
    val query : String,
    
    @Schema(
        description = "Resource type to filter search results. Defaults to DATASET if not specified. Use ALL to search across all resource types. Valid values: ALL, CONCEPT, DATASET, DATA_SERVICE, INFORMATION_MODEL, SERVICE, EVENT",
        example = "DATASET",
        allowableValues = ["ALL", "CONCEPT", "DATASET", "DATA_SERVICE", "INFORMATION_MODEL", "SERVICE", "EVENT"],
        required = false,
        defaultValue = "DATASET"
    )
    val type : SearchType = SearchType.DATASET
)
