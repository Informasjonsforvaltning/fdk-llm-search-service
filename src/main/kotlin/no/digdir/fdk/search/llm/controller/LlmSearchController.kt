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
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping(value = ["/llm"], produces = ["application/json"])
class LlmSearchController(
    private val llmSearchService: LlmSearchService
) {
    @PostMapping
    fun search(@RequestBody query: LlmSearchOperation): ResponseEntity<LlmSearchResult> =
        try {
            ResponseEntity(
                llmSearchService.search(query),
                HttpStatus.OK
            )
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
        }
}

