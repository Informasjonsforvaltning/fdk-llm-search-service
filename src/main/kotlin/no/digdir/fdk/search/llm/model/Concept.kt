package no.digdir.fdk.search.llm.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Concept(
    val identifier: String?,
    val collection: Collection?,
    val publisher: Organization?,
    val definition: Definition?,
    val prefLabel: LocalizedStrings?,
    val harvest: HarvestMetadata?,
    val associativeRelation: List<AssociativeRelation>?,
    val closeMatch: List<String>?,
    val exactMatch: List<String>?,
    val genericRelation: List<GenericRelation>?,
    val isReplacedBy: List<String>?,
    val memberOf: List<String>?,
    val partitiveRelation: List<PartitiveRelation>?,
    val replaces: List<String>?,
    val seeAlso: List<String>?,
    val hiddenLabel: List<LocalizedStrings>?,
    val altLabel: List<LocalizedStrings>?
)

