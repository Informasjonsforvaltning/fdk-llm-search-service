package no.digdir.fdk.search.llm.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class InformationModel(
    val uri: String?,
    val title: LocalizedStrings?,
    val catalog: Catalog?,
    val description: LocalizedStrings?,
    val keyword: List<LocalizedStrings>?,
    val theme: List<EuDataTheme>?,
    val losTheme: List<LosNode>?,
    val publisher: Organization?,
    val accessRights: ReferenceDataCode?,
    val harvest: HarvestMetadata?,
    val hasPart: String?,
    val isPartOf: String?,
    val isReplacedBy: String?,
    val replaces: String?,
    val subjects: List<String>?
)

