package no.digdir.fdk.search.llm.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = "A single search result hit containing dataset information and relevance explanation"
)
data class LlmSearchHit(
    @Schema(
        description = "Unique identifier of the dataset",
        example = "12345"
    )
    val id : String,
    
    @Schema(
        description = "Title of the dataset",
        example = "Befolkningsstatistikk"
    )
    val title : String,
    
    @Schema(
        description = "Explanation of why this dataset matches the search query, formatted in Markdown",
        example = "Dette datasettet inneholder statistikk om befolkning og demografi i Norge, inkludert aldersfordeling, kjønnsfordeling og geografisk fordeling."
    )
    val description : String,
    
    @Schema(
        description = "Type of resource (e.g., 'dataset', 'dataservice')",
        example = "dataset"
    )
    val type : String,
    
    @Schema(
        description = "Name of the organization that published the dataset",
        example = "Statistisk sentralbyrå"
    )
    val publisher : String,
    
    @Schema(
        description = "Unique identifier of the publishing organization",
        example = "991825827"
    )
    val publisherId: String,
)

@Schema(
    description = "Search result containing a list of relevant datasets matching the query",
    example = """{
  "hits": [
    {
      "id": "12345",
      "title": "Befolkningsstatistikk",
      "description": "Dette datasettet inneholder statistikk om befolkning og demografi i Norge.",
      "type": "dataset",
      "publisher": "Statistisk sentralbyrå",
      "publisherId": "991825827"
    }
  ]
}"""
)
data class LlmSearchResult(
    @Schema(
        description = "List of search hits, ordered by relevance",
        required = true
    )
    val hits : List<LlmSearchHit>
)
