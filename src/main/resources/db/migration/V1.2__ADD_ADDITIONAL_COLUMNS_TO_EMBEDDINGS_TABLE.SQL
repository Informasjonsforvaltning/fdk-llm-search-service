ALTER TABLE embeddings
    ADD COLUMN timestamp BIGINT,
    ADD COLUMN deleted BOOLEAN;
