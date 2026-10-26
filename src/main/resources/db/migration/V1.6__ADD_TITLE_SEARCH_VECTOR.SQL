-- Full-text search vector on the title, used together with the embedding in hybrid search
ALTER TABLE embeddings
    ADD COLUMN title_tsv tsvector
        GENERATED ALWAYS AS (to_tsvector('norwegian', coalesce(metadata->>'title', ''))) STORED;

CREATE INDEX title_tsv_index ON embeddings USING gin (title_tsv);
