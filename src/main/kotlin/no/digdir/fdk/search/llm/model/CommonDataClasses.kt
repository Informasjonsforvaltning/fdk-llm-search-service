package no.digdir.fdk.search.llm.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class LocalizedStrings(
    val nb: String?,
    val nn: String?,
    val no: String?,
    val en: String?
)

fun LocalizedStrings.valueByPriority(): String? {
    return nb ?: no ?: nn ?: en
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class EuDataTheme(
    val title: LocalizedStrings?,
    val code: String?
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class LosNode(
    val name: LocalizedStrings?,
    val losPaths: List<String>?
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ReferenceDataCode(
    val uri: String?,
    val code: String?,
    val prefLabel: LocalizedStrings?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Reference(
    val referenceType: ReferenceDataCode?,
    val source: ObjectWithURI?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ObjectWithURI(
    val uri: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AssociativeRelation(
    val description: LocalizedStrings?,
    val related: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GenericRelation(
    val divisioncriterion: LocalizedStrings?,
    val generalizes: String?,
    val specializes: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PartitiveRelation(
    val description: LocalizedStrings?,
    val hasPart: String?,
    val isPartOf: String?
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class TextAndURI(
    val text: LocalizedStrings?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Definition(
    val text: LocalizedStrings?,
    val sources: List<TextAndURI>?,
    val sourceRelationship: String?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class HarvestMetadata(
    val firstHarvested: String?,
    val modified: String?
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Metadata(
    val firstHarvested: String?,
    val modified: String?,
    val deleted: Boolean?,
    val timestamp: Long?
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Catalog(
    val description: LocalizedStrings?,
    val id: String?,
    val publisher: Organization?,
    val title: LocalizedStrings?,
    val uri: String?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Collection(
    val description: LocalizedStrings?,
    val id: String?,
    val publisher: Organization?,
    val label: LocalizedStrings?,
    val uri: String?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Organization(
    val id: String?,
    val uri: String?,
    val orgPath: String?,
    val name: String?,
    val prefLabel: LocalizedStrings?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Distribution(
    val fdkFormat: List<MediaTypeOrExtent>?
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class MediaTypeOrExtent(
    val uri: String?,
    val name: String?,
    val code: String?,
    val type: MediaTypeOrExtentType = MediaTypeOrExtentType.UNKNOWN
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Temporal(
    val uri: String?,
    val startDate: String?,
    val endDate: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AIResultHit(
    val id: String,
    val name: String,
    val reason: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AIResult(
    val sensitive: Boolean,
    val hits: List<AIResultHit>
)
