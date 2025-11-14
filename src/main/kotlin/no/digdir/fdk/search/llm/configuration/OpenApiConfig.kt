package no.digdir.fdk.search.llm.configuration

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class OpenApiConfig {
    @Bean
    open fun openAPI(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("FDK LLM Search Service API")
                    .description(
                        """
                        A REST API providing intelligent, context-aware search capabilities for Norway's national data catalog using Large Language Models (LLM).
                        
                        ## About Data.norge.no
                        This API provides LLM-powered search functionality for the data catalog published on [data.norge.no](https://data.norge.no), operated by the Norwegian Digitalisation Agency (Digitaliseringsdirektoratet). The catalog serves as Norway's central registry for public sector data resources, promoting fair competition and enabling data reuse for both commercial and non-commercial purposes.
                        
                        ## What This Service Provides
                        This service enhances the data discovery experience by enabling natural language, free-text searches across the data catalog. Unlike traditional keyword-based search, this API uses advanced AI to understand the context and intent behind search queries, making it easier to find relevant datasets even when using conversational language.
                        
                        ## How It Works
                        The search process consists of two main steps:
                        
                        1. **Document Retrieval**: Uses text embeddings and vector similarity search to find potentially relevant dataset descriptions from the catalog
                        2. **LLM Filtering and Validation**: Uses Google Vertex AI to intelligently filter results and provide contextual explanations for why each dataset matches the query
                        
                        The entire process typically completes in approximately 5 seconds, with most time spent on LLM processing.
                        
                        ## API Capabilities
                        - **Natural Language Search**: Query the catalog using free-form, conversational language in Norwegian
                        - **Contextual Understanding**: The LLM understands intent and context, not just keywords
                        - **Relevance Scoring**: Results include explanations of why each dataset matches your query
                        - **Sensitivity Detection**: Automatically identifies if queries might involve personal sensitive data
                        - **Metadata Enrichment**: Returns comprehensive metadata including titles, descriptions, types, publishers, and more
                        
                        ## Use Cases
                        - **Intelligent Data Discovery**: Find datasets using natural language queries instead of exact keywords
                        - **Contextual Search**: Discover relevant data even when you're not sure of the exact terminology
                        - **Application Integration**: Integrate intelligent search capabilities into applications and systems
                        - **Data Exploration**: Explore the catalog using conversational queries to understand available resources
                        
                        ## Technical Details
                        - **LLM Provider**: Google Vertex AI (Gemini models)
                        - **Vector Search**: PostgreSQL with pgvector extension for similarity search
                        - **Embeddings**: Text embeddings generated using Vertex AI
                        - **Response Format**: JSON with structured results including relevance explanations
                        
                        ## Data Governance
                        Content is provided by the organizations themselves, with each organization responsible for managing their content in the catalogs. The Norwegian Digitalisation Agency is responsible for the operation and development of the platform.
                        
                        All search queries are logged for analysis and service improvement purposes. No personally identifiable information is stored, and queries cannot be traced back to individual users.
                        
                        For more information about finding and using data, visit [data.norge.no](https://data.norge.no/nb/docs/finding-data) or learn more [about the platform](https://data.norge.no/nb/about).
                        """.trimIndent(),
                    ).version("1.0.0"),
            )
}

