package no.digdir.fdk.search.llm.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class Event(
    val uri: String?,
    val title: LocalizedStrings?,
    val catalog: Catalog?,
    val description: LocalizedStrings?,
    val harvest: HarvestMetadata?,
    val subject: List<String>?,
    @JsonProperty("specialized_type") val specializedType: String?,
)

