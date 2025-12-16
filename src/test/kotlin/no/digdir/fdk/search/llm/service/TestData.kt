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

val TEST_CONCEPT_ALL_FIELDS = Concept(
    identifier = "concept-123",
    collection = Collection(
        description = LocalizedStrings("NB collection desc", null, null, null),
        id = "collection-id",
        publisher = Organization(
            id = "pub-id",
            uri = "pub-uri",
            orgPath = "/ORG/123",
            name = "Publisher Name",
            prefLabel = LocalizedStrings("NB Publisher", null, null, null)
        ),
        label = LocalizedStrings("NB Collection", null, null, null),
        uri = "collection-uri"
    ),
    publisher = Organization(
        id = "concept-pub-id",
        uri = "concept-pub-uri",
        orgPath = "/ORG/456",
        name = "Concept Publisher",
        prefLabel = LocalizedStrings("NB Concept Publisher", null, null, null)
    ),
    definition = Definition(
        text = LocalizedStrings("NB Concept definition", null, null, null),
        sources = null,
        sourceRelationship = null
    ),
    prefLabel = LocalizedStrings("NB Concept Label", null, null, null),
    harvest = HarvestMetadata(
        firstHarvested = "2021-01-01",
        modified = "2021-01-02"
    ),
    associativeRelation = null,
    closeMatch = null,
    exactMatch = null,
    genericRelation = null,
    isReplacedBy = null,
    memberOf = null,
    partitiveRelation = null,
    replaces = null,
    seeAlso = null,
    hiddenLabel = listOf(LocalizedStrings("NB Hidden Label", null, null, null)),
    altLabel = listOf(LocalizedStrings("NB Alt Label", null, null, null))
)

val TEST_DATA_SERVICE_ALL_FIELDS = DataService(
    uri = "dataservice-uri",
    title = LocalizedStrings("NB DataService Title", null, null, null),
    catalog = Catalog(
        description = LocalizedStrings("NB catalog desc", null, null, null),
        id = "catalog-id",
        publisher = Organization(
            id = "catalog-pub-id",
            uri = "catalog-pub-uri",
            orgPath = "/ORG/789",
            name = "Catalog Publisher",
            prefLabel = LocalizedStrings("NB Catalog Publisher", null, null, null)
        ),
        title = LocalizedStrings("NB Catalog", null, null, null),
        uri = "catalog-uri"
    ),
    description = LocalizedStrings("NB DataService description", null, null, null),
    keyword = listOf(LocalizedStrings("keyword1", null, null, null)),
    theme = listOf(
        EuDataTheme(
            title = LocalizedStrings("theme1", null, null, null),
            code = "T1"
        )
    ),
    losTheme = listOf(
        LosNode(
            name = LocalizedStrings("lostheme1", null, null, null),
            losPaths = listOf("path1")
        )
    ),
    publisher = Organization(
        id = "dataservice-pub-id",
        uri = "dataservice-pub-uri",
        orgPath = "/ORG/101",
        name = "DataService Publisher",
        prefLabel = LocalizedStrings("NB DataService Publisher", null, null, null)
    ),
    accessRights = ReferenceDataCode(
        uri = "access-uri",
        code = "PUBLIC",
        prefLabel = LocalizedStrings("public", null, null, null)
    ),
    harvest = HarvestMetadata(
        firstHarvested = "2021-01-01",
        modified = "2021-01-02"
    ),
    conformsTo = null,
    servesDataset = listOf("dataset1", "dataset2"),
    fdkFormat = listOf(MediaTypeOrExtent(code = "json", type = MediaTypeOrExtentType.MEDIA_TYPE, name = "json", uri = "json-uri"))
)

val TEST_INFORMATION_MODEL_ALL_FIELDS = InformationModel(
    uri = "infomodel-uri",
    title = LocalizedStrings("NB InformationModel Title", null, null, null),
    catalog = Catalog(
        description = LocalizedStrings("NB catalog desc", null, null, null),
        id = "catalog-id",
        publisher = Organization(
            id = "catalog-pub-id",
            uri = "catalog-pub-uri",
            orgPath = "/ORG/789",
            name = "Catalog Publisher",
            prefLabel = LocalizedStrings("NB Catalog Publisher", null, null, null)
        ),
        title = LocalizedStrings("NB Catalog", null, null, null),
        uri = "catalog-uri"
    ),
    description = LocalizedStrings("NB InformationModel description", null, null, null),
    keyword = listOf(LocalizedStrings("keyword1", null, null, null)),
    theme = listOf(
        EuDataTheme(
            title = LocalizedStrings("theme1", null, null, null),
            code = "T1"
        )
    ),
    losTheme = listOf(
        LosNode(
            name = LocalizedStrings("lostheme1", null, null, null),
            losPaths = listOf("path1")
        )
    ),
    publisher = Organization(
        id = "infomodel-pub-id",
        uri = "infomodel-pub-uri",
        orgPath = "/ORG/202",
        name = "InformationModel Publisher",
        prefLabel = LocalizedStrings("NB InformationModel Publisher", null, null, null)
    ),
    accessRights = ReferenceDataCode(
        uri = "access-uri",
        code = "PUBLIC",
        prefLabel = LocalizedStrings("public", null, null, null)
    ),
    harvest = HarvestMetadata(
        firstHarvested = "2021-01-01",
        modified = "2021-01-02"
    ),
    hasPart = null,
    isPartOf = null,
    isReplacedBy = null,
    replaces = null,
    subjects = listOf("subject1", "subject2")
)

val TEST_SERVICE_ALL_FIELDS = no.digdir.fdk.search.llm.model.Service(
    uri = "service-uri",
    title = LocalizedStrings("NB Service Title", null, null, null),
    catalog = Catalog(
        description = LocalizedStrings("NB catalog desc", null, null, null),
        id = "catalog-id",
        publisher = Organization(
            id = "catalog-pub-id",
            uri = "catalog-pub-uri",
            orgPath = "/ORG/789",
            name = "Catalog Publisher",
            prefLabel = LocalizedStrings("NB Catalog Publisher", null, null, null)
        ),
        title = LocalizedStrings("NB Catalog", null, null, null),
        uri = "catalog-uri"
    ),
    description = LocalizedStrings("NB Service description", null, null, null),
    keyword = listOf(LocalizedStrings("keyword1", null, null, null)),
    euDataThemes = listOf(
        EuDataTheme(
            title = LocalizedStrings("theme1", null, null, null),
            code = "T1"
        )
    ),
    losTheme = listOf(
        LosNode(
            name = LocalizedStrings("lostheme1", null, null, null),
            losPaths = listOf("path1")
        )
    ),
    ownedBy = listOf(
        ServiceOrganization(
            identifier = "owner-id",
            uri = "owner-uri",
            orgPath = "/ORG/303",
            prefLabel = LocalizedStrings("NB Owner Name", null, null, null),
            title = LocalizedStrings("NB Owner Title", null, null, null)
        )
    ),
    hasCompetentAuthority = listOf(
        ServiceOrganization(
            identifier = "authority-id",
            uri = "authority-uri",
            orgPath = "/ORG/404",
            prefLabel = LocalizedStrings("NB Authority Name", null, null, null),
            title = LocalizedStrings("NB Authority Title", null, null, null)
        )
    ),
    spatial = listOf("Oslo", "Bergen"),
    harvest = HarvestMetadata(
        firstHarvested = "2021-01-01",
        modified = "2021-01-02"
    ),
    isGroupedBy = null,
    isClassifiedBy = null,
    isDescribedAt = null,
    relation = null,
    requires = null,
    subject = null
)

val TEST_EVENT_ALL_FIELDS = Event(
    uri = "event-uri",
    title = LocalizedStrings("NB Event Title", null, null, null),
    catalog = Catalog(
        description = LocalizedStrings("NB catalog desc", null, null, null),
        id = "catalog-id",
        publisher = Organization(
            id = "catalog-pub-id",
            uri = "catalog-pub-uri",
            orgPath = "/ORG/789",
            name = "Catalog Publisher",
            prefLabel = LocalizedStrings("NB Catalog Publisher", null, null, null)
        ),
        title = LocalizedStrings("NB Catalog", null, null, null),
        uri = "catalog-uri"
    ),
    description = LocalizedStrings("NB Event description", null, null, null),
    harvest = HarvestMetadata(
        firstHarvested = "2021-01-01",
        modified = "2021-01-02"
    ),
    subject = listOf("subject1", "subject2"),
    specializedType = "LIFE_EVENT"
)

