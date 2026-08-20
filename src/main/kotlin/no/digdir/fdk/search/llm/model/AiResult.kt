package no.digdir.fdk.search.llm.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class AIResultHit(val id: String, val name: String, val reason: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AIResult(val sensitive: Boolean, val hits: List<AIResultHit>)
