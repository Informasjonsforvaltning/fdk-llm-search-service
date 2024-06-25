CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE embeddings(
    id VARCHAR(200) NOT NULL,
    content TEXT,
    embedding vector(768),
    metadata JSONB,
    PRIMARY KEY (id)
);

CREATE INDEX ON embeddings
    USING hnsw(embedding vector_cosine_ops)
    WITH (m = 24, ef_construction = 100);

CREATE INDEX metadatagin ON embeddings USING gin (metadata);
