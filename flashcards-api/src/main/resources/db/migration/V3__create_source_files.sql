-- V3: arquivos de origem (metadados; binario em object storage)
CREATE TABLE source_files (
    id                UUID         NOT NULL PRIMARY KEY,
    original_filename VARCHAR(255) NOT NULL,
    content_type      VARCHAR(120),
    size_bytes        BIGINT       NOT NULL,
    storage_key       VARCHAR(512) NOT NULL,
    uploaded_at       TIMESTAMP    NOT NULL,
    created_at        TIMESTAMP    NOT NULL,
    updated_at        TIMESTAMP    NOT NULL
);

CREATE INDEX idx_source_files_uploaded_at ON source_files (uploaded_at);
