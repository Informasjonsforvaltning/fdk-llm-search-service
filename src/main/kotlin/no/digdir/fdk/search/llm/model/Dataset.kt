package no.digdir.fdk.search.llm.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Dataset(
    val uri: String,
    val title: LocalizedStrings?,
    val catalog: Catalog?,
    val description: LocalizedStrings?,
    val keyword: List<LocalizedStrings>?,
    val theme: List<EuDataTheme>?,
    val losTheme: List<LosNode>?,
    val publisher: Organization?,
    val accessRights: ReferenceDataCode?,
    val isOpenData: Boolean?,
    val spatial: List<ReferenceDataCode>?,
    val provenance: ReferenceDataCode?,
    val harvest: HarvestMetadata?,
    val distribution: List<Distribution>?,
    val conformsTo: List<ObjectWithURI>?,
    val inSeries: ObjectWithURI?,
    val references: List<Reference>?,
    val subject: List<ObjectWithURI>?,
    val specializedType: String?,
    val isAuthoritative: Boolean?,
    val isRelatedToTransportportal: Boolean?,
    val issued: String?,
    val accrualPeriodicity: ReferenceDataCode?,
    val temporal: List<Temporal>?,
)
