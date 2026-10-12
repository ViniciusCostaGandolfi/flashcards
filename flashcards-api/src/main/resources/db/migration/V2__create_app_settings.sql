-- V2: configuracoes globais (linha singleton)
CREATE TABLE app_settings (
    id                    UUID         NOT NULL PRIMARY KEY,
    reader_model          VARCHAR(120),
    generator_model       VARCHAR(120),
    base_prompt           TEXT,
    cards_per_generation  INTEGER      NOT NULL DEFAULT 6,
    language              VARCHAR(20)  NOT NULL DEFAULT 'pt-BR',
    pdf_engine            VARCHAR(30)  NOT NULL DEFAULT 'native',
    created_at            TIMESTAMP    NOT NULL,
    updated_at            TIMESTAMP    NOT NULL
);
