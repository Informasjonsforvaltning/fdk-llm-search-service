package no.digdir.fdk.search.llm.controller

import no.digdir.fdk.search.llm.model.LlmSearchOperation
import no.digdir.fdk.search.llm.model.LlmSearchResult
import no.digdir.fdk.search.llm.service.LlmSearchService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(value = ["/llm"], produces = ["application/json"])
class LlmSearchController(
    private val llmSearchService: LlmSearchService
) {
    @PostMapping
    fun search(@RequestBody query: LlmSearchOperation): ResponseEntity<LlmSearchResult> =
        ResponseEntity(
            llmSearchService.search(query),
            HttpStatus.OK
        )
}

