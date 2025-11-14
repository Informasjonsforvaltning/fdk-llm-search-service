package no.digdir.fdk.search.llm.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import no.digdir.fdk.search.llm.model.LlmSearchOperation
import no.digdir.fdk.search.llm.model.LlmSearchResult
import no.digdir.fdk.search.llm.service.LlmSearchService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping(value = ["/llm"], produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(
    name = "LLM Search",
    description = "API for performing intelligent, context-aware searches across the data catalog using Large Language Models"
)
class LlmSearchController(
    private val llmSearchService: LlmSearchService
) {
    @PostMapping
    @Operation(
        summary = "Perform LLM-powered search",
        description = """
            Performs an intelligent, context-aware search across the data catalog using natural language queries.
            
            The search process uses vector similarity search combined with LLM filtering to find and rank relevant datasets.
            Results include explanations of why each dataset matches the query, making it easier to understand relevance.
            
            **Query Requirements:**
            - Minimum length: 3 characters
            - Maximum length: 255 characters
            - Supports natural language queries in Norwegian
            
            **Response Time:**
            Typical response time is approximately 5 seconds, with most time spent on LLM processing.
        """.trimIndent(),
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully performed search and retrieved results",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = LlmSearchResult::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid request - query does not meet length requirements (must be between 3 and 255 characters)",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE)],
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error - search processing failed",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE)],
            ),
        ],
    )
    fun search(
        @Parameter(
            description = "Search operation containing the natural language query to search for",
            required = true,
            schema = Schema(implementation = LlmSearchOperation::class),
        )
        @RequestBody query: LlmSearchOperation
    ): ResponseEntity<LlmSearchResult> =
        try {
            ResponseEntity(
                llmSearchService.search(query),
                HttpStatus.OK
            )
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
        }
}

