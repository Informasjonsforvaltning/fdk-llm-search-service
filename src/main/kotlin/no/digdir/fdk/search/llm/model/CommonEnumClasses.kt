package no.digdir.fdk.search.llm.model

enum class SearchType {
    CONCEPT,
    DATASET,
    DATA_SERVICE,
    INFORMATION_MODEL,
    SERVICE,
    EVENT,
}

enum class MediaTypeOrExtentType {
    UNKNOWN,
    MEDIA_TYPE,
    FILE_TYPE
}

enum class SpecializedType {
    DATASET_SERIES,
    LIFE_EVENT,
    BUSINESS_EVENT,
}
