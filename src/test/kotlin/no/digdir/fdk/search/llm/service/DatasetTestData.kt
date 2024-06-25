package no.digdir.fdk.search.llm.service

import no.digdir.fdk.search.llm.model.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val TEST_NULL_DATASET = Dataset(
    uri = "test uri",
    title = null,
    description = null,
    keyword = null,
    theme = null,
    losTheme = null,
    publisher = null,
    accessRights = null,
    isOpenData = false,
    spatial = null,
    provenance = null,
    harvest = null,
    catalog = null,
    distribution = null,
    subject = null,
    conformsTo = null,
    inSeries = null,
    references = null,
    specializedType = null,
    isAuthoritative = null,
    isRelatedToTransportportal = null,
    issued = null,
    accrualPeriodicity = null,
    temporal = null
)

val TEST_DATASET_ALL_FIELDS = TEST_NULL_DATASET.copy(
    title = LocalizedStrings(
        "NB Test title",
        "NN Test title",
        "NO Test title",
        "EN Test title"),
    description = LocalizedStrings(
        "description",
        "description",
        "description",
        "description"),
    keyword = listOf(
        LocalizedStrings(
            "keyword",
            "keyword",
            "keyword",
            "keyword")
    ),
    theme = listOf(
        EuDataTheme(
            title = LocalizedStrings(
                "eutheme1",
                null,
                null,
                null),
            code = "EU1"),
        EuDataTheme(
            title = LocalizedStrings(
                "eutheme2",
                null,
                null,
                null),
            code = "EU2")
    ),
    losTheme = listOf(
        LosNode(
            name = LocalizedStrings(
                "lostheme1",
                null,
                null,
                null),
            losPaths = listOf("los-path"),
        )
    ),
    isRelatedToTransportportal = true,
    publisher = Organization(
        orgPath = "/STAT/272417858",
        id = "Test publisher > identifier",
        uri = "Test publisher > uri",
        name = "Test publisher > name",
        prefLabel = LocalizedStrings(
            "NB Test publisher > prefLabel",
            "NN Test publisher > prefLabel",
            "NO Test publisher > prefLabel",
            "EN Test publisher > prefLabel"),
    ),
    accessRights = ReferenceDataCode(
        uri = "accessRights > uri",
        code = "PUBLIC",
        prefLabel = LocalizedStrings(
            "public",
            "public",
            "public",
            "public")
    ),
    spatial = listOf(
        ReferenceDataCode(
            uri = "spatial > uri",
            code = "Test spatial > code",
            prefLabel = LocalizedStrings(
                "NB Test spatial > prefLabel",
                "NN Test spatial > prefLabel",
                "NO Test spatial > prefLabel",
                "EN Test spatial > prefLabel")
        )
    ),
    provenance = ReferenceDataCode(
        uri = "provenance > uri",
        code = "Test provenance > code",
        prefLabel = LocalizedStrings(
            "NB Test provenance > prefLabel",
            "NN Test provenance > prefLabel",
            "NO Test provenance > prefLabel",
            "EN Test provenance > prefLabel")
    ),
    harvest = HarvestMetadata(
        firstHarvested = LocalDateTime.now().minusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE),
        modified = LocalDateTime.now().minusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE)
    ),
    catalog = Catalog(
        description = LocalizedStrings(
            "NB Test catalog > description",
            "NN Test catalog > description",
            "NO Test catalog > description",
            "EN Test catalog > description"),
        id = "Test catalog > id",
        uri = "Test catalog > uri",
        title = LocalizedStrings(
            "NB Test catalog > title",
            "NN Test catalog > title",
            "NO Test catalog > title",
            "EN Test catalog > title"),
        publisher = Organization(
            orgPath = "/PRIVAT/172417858",
            id = "Test publisher > identifier",
            uri = "Test publisher > uri",
            name = "Test publisher > name",
            prefLabel = LocalizedStrings(
                "publisher",
                "publisher",
                "publisher",
                "publisher"),
        )
    ),
    issued = "2021-01-01",
    accrualPeriodicity = ReferenceDataCode(
        uri = "accrualPeriodicity > uri",
        code = "Test accrualPeriodicity > code",
        prefLabel = LocalizedStrings(
            "NB Test accrualPeriodicity > prefLabel",
            "NN Test accrualPeriodicity > prefLabel",
            "NO Test accrualPeriodicity > prefLabel",
            "EN Test accrualPeriodicity > prefLabel")
    ),
    temporal = listOf(
        Temporal(
            uri = "temporal > uri",
            startDate = "2021-01-01",
            endDate = "2021-12-31"
        )
    ),
    distribution = listOf(
        Distribution(fdkFormat = listOf(MediaTypeOrExtent(code = "json", type = MediaTypeOrExtentType.MEDIA_TYPE, name = "json", uri = "json > uri")))
    )
)
