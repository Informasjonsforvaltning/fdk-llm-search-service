CREATE TABLE search_queries(
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    query VARCHAR(255) NOT NULL,
    hits_embedding INT NOT NULL,
    hits_llm INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
