-- V6: jobs de geracao assincrona
CREATE TABLE generation_jobs (
    id             UUID        NOT NULL PRIMARY KEY,
    user_id        UUID        NOT NULL,
    source_file_id UUID,
    status         VARCHAR(20) NOT NULL,
    error_message  TEXT,
    deck_id        UUID,
    source_type    VARCHAR(20) NOT NULL,
    input_text     TEXT,
    title          VARCHAR(255),
    created_at     TIMESTAMP   NOT NULL,
    updated_at     TIMESTAMP   NOT NULL,
    CONSTRAINT fk_generation_jobs_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_generation_jobs_source_file FOREIGN KEY (source_file_id) REFERENCES source_files (id),
    CONSTRAINT fk_generation_jobs_deck FOREIGN KEY (deck_id) REFERENCES decks (id)
);

CREATE INDEX idx_generation_jobs_user ON generation_jobs (user_id);
CREATE INDEX idx_generation_jobs_status ON generation_jobs (status);
