# FDK LLM Search Service

This application provides an API and uses a Large Language Model (LLM) that builds on metadata
from [data.norge](https://data.norge.no) to allow contextual free-text searches to simplify the process of finding
datasets described on data.norge.no.

For a broader understanding of the system’s context, refer to
the [architecture documentation](https://github.com/Informasjonsforvaltning/architecture-documentation) wiki. For more
specific context on this application, see the **Portal** subsystem section.

## Requirements

- maven
- java 21
- docker
- docker-compose

## Generate sources

Kafka messages are serialized using Avro. Avro schemas are located in the kafka/schemas directory.
To generate sources from Avro schema, run the following command:

```
mvn generate-sources
```

## Run tests

```
mvn test
```

## Run locally

### Start PostgreSQL database, Kafka cluster and setup topics/schemas

Topics and schemas are set up automatically when starting the Kafka cluster.
Docker compose uses the scripts create-topics.sh and create-schemas.sh to set up topics and schemas.

```
docker-compose up -d
```

If you have problems starting kafka, check if all health checks are ok.
Make sure number at the end (after 'grep') matches desired topics.

### Start search service

Start search service locally using maven. Use Spring profile **develop**.

```
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Produce messages

Check if schema id is correct in the script. This should be 1 if there
is only one schema in your registry.

```
sh ./kafka/produce-rdf-parse-events.sh
sh ./kafka/produce-dataset-remove-events.sh
```

## Teknologier

FDK LLM Search Service kjører på Google Cloud Platform (GCP) og benytter seg av Google sine
tilbydde tjenester. LLM-en vi benytter oss av er [Vertex AI](https://cloud.google.com/vertex-ai).
Vi forholder oss utelukkende til bruk av context når vi benytter Vertex og
gjør dermed ingen form for fine-tuning av modellen. Dette sparer oss for masse
tid og kostnader assosiert med modell-trening.

I tillegg bruker vi [LangChain](https://python.langchain.com/docs/get_started/introduction),
som rammeverk for å opprette prompt-templates og interagere med Vertex.

Datagrunnlaget for LLM-modellen ligger i en PostgreSQL instanse som kjører på
Cloud SQL i GCP. Helt kritisk for applikasjonen er postgres-utvidelsen [pgvector](https://github.com/pgvector/pgvector),
som lar oss gjøre vektoriserte likhetssøk mot datagrunnlaget vårt.

## Teknisk arkitektur

Arkitekturern til FDK-LLM er under kontinuerlig utvikling kan endre seg raskt.
Hovedsaklig kan et søk deles opp i 2 steg. Disse er som følger:

- Dokumentgjennfinning
- LLM filtrering og validering

Disse to stegene kan gjennomføres ende-til-ende på omlag 5 sekunder.
Tidsmessig er det spørringen mot Vertex som tar størsteparten av tiden, mens
spørringen mot postgres tar en brøkdel av et sekund.

### Datatilberedninger

Tjenesten lytter på rdf-parse-events (Kafka) og oppdaterer embeddings i
postgres-databasen. Vi bruker `pgvector` til å gjøre søk.

Pgvector bruker text-embeddings til å sammenlikne datasettbeskrivelsene. Dette betyr at
vi må ha datasettbeskrivelsene på et rent tekstlig format. For å oppnå dette lager vi
en tekstlig oppsummering av hver enkelt beskrivelse som vi deretter
vektoriserer ved hjelp av Vertex. Nedenfor finner du et eksempel av hvordan
en slik oppsummering ser ut.

```
Dette datasettet, med id '12345' og navn 'Mitt datasett' er utgitt av 'Organisasjon A'.
Datasettet har public tilgang.

Beskrivelsen av datasettet er som følger:
Her finner man beskrivelsen av datasettet.

Datasettet ble utgitt 01.01.2021 og oppdateres ukentlig.
Datasettet har 2 distribusjoner og tilbyr data på formatene json.
Temaene for datasettet er: tema1, tema2, tema3.
Nøkkelordene for datasettet er: nøkkelordet.
Dataen er tidsmessig begrenset: 2021-01-01 til 2021-12-31.
```

### Dokumentgjennfinning

I første steg av prosessen finner vi potensielt relevante datasettbeskrivelser
ut ifra søket. Vi bruker Text Embedding i Vertex til å vektorisere søket, og kjører
et likhetssøk mot vektorene i databasen.

```postgresql
 SELECT id,
        content,
        metadata,
        1 - (embedding <=> $1) AS similarity
 FROM dataset_embeddings
 WHERE 1 - (embedding <=> $1) > $2
 ORDER BY similarity DESC
 LIMIT $3
```

I tillegg til vektoren av søketeksten bruker vi to ekstra variabler for å
kontrollere likhetsterskel og maks antall returnerte datasettbeskrivelser.
Antallet returnert i dette steget har stor innvirkning på oppfattet nøyaktighet
på brukersiden. Vi vil gjerne ha så mange datasettbeskrivelser som mulig, men
blir det for mange kan det gjøre neste steg en del tregere. I tillegg er det en
hard grense på hvor mange vi kan ta siden Vertex har en grense på størrelse
av context. Vi har landet på maks 7 datasettbeskrivelser i vårt use-case, men
dette kan endre seg i fremtiden. Vi lagrer metadata som jsonb i tillegg til vektoren
for å kunne filtrere bort irrelevante embeddings eller hente ut informasjon
som tittel og utgiver.

### LLM filtrering og validering

Siste steg av prosessen bruker Vertex sin LLM til å filtrere bort
irrelevante datasettbeskrivelser fra forrige steg og besvare forespørselen fra brukeren.
Her bruker vi LangChain til å opprette en prompt som tar inn
tekstlige oppsummeringer av datasettbeskrivelser og gir instruksjoner om hvordan
forespørselen skal behandles. En typisk spørring mot Vertex vil se ut
som beskrevet under:

````markdown
You will be given a detailed summaries of different datasets in norwegian as a JSON array.
The question is enclosed in double backticks(``).
Select all datasets that are relevant to answer the question.
Prioritize datasets with newer data.
Using those dataset summaries, answer the question in as much detail as possible.
Give your answer in Norwegian.
You should only use the information in the summaries.
Your answer should start with explaining if the question contains possible personal sensitive data
(sensitive) and why each dataset match the question posed by the user (reason).
Format the result as JSON only using the following structure format the description in Markdown:

```json
{ "sensitive": true/false, "hits": [ { "id": "", "name": "", "reason": "" } ] }
```

Summaries:

```json
{{summaries}}
```

Question:
``{{user_query}}``
````

### Forbedringer

For å øke nøyaktigheten i søket har vi forsøkt å bruke LLMen til å generere
relevante, ekstra nøkkelord som så brukes til å utvide søket i PostgreSQL.
Resultatet av dette har vært bedre på mindre detaljerte forespørsler, på
bekostning av cirka 1 sekund ekstra behandlingstid. Dette er
ikke aktivert per i dag.

### Lagring av brukerdata

Tjenesten lagrer alle spørringer og antall treff i en PostgreSQL database. Dataene
lagres i en tabell med følgende skjema:

```postgresql
CREATE TABLE search_queries
(
    id             uuid         NOT NULL DEFAULT gen_random_uuid(),
    query          VARCHAR(255) NOT NULL,
    hits_embedding INT          NOT NULL,
    hits_llm       INT          NOT NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
```

Dataene lagres for å kunne gjøre analyser på søkemønstre og for å kunne
forbedre søketjenesten i fremtiden. Dataene kan ikke knytes til enkeltpersoner.
Utover dette lagres det ingen personopplysninger i tjenesten.

Mer om Google generative AI og Data Governance kan leses her:

- https://cloud.google.com/vertex-ai/generative-ai/docs/data-governance#prediction
