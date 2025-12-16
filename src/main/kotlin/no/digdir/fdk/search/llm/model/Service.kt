package no.digdir.fdk.search.llm.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Service(
    val uri: String?,
    val title: LocalizedStrings?,
    val catalog: Catalog?,
    val description: LocalizedStrings?,
    val keyword: List<LocalizedStrings>?,
    val euDataThemes: List<EuDataTheme>?,
    val losTheme: List<LosNode>?,
    val ownedBy: List<ServiceOrganization>?,
    val hasCompetentAuthority: List<ServiceOrganization>?,
    val spatial: List<String>?,
    val harvest: HarvestMetadata?,
    val isGroupedBy: List<String>?,
    val isClassifiedBy: List<ObjectWithURI>?,
    val isDescribedAt: List<ObjectWithURI>?,
    val relation: List<ObjectWithURI>?,
    val requires: List<ObjectWithURI>?,
    val subject: List<ObjectWithURI>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ServiceOrganization(
    val identifier: String?,
    val uri: String?,
    val orgPath: String?,
    val prefLabel: LocalizedStrings?,
    val title: LocalizedStrings?,
)

