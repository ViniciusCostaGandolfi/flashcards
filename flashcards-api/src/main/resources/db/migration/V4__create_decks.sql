-- V4: decks
CREATE TABLE decks (
    id              UUID         NOT NULL PRIMARY KEY,
    title           VARCHAR(255) NOT NULL,
    source_type     VARCHAR(20)  NOT NULL,
    source_file_id  UUID,
    extracted_text  TEXT,
    reader_model    VARCHAR(120),
    generator_model VARCHAR(120),
    prompt_used     TEXT,
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP    NOT NULL,
    CONSTRAINT fk_decks_source_file FOREIGN KEY (source_file_id) REFERENCES source_files (id)
);

CREATE INDEX idx_decks_created_at ON decks (created_at);
CREATE INDEX idx_decks_source_file ON decks (source_file_id);
