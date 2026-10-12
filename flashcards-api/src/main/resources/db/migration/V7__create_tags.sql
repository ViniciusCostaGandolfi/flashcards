-- V7: tags + card_tags (ManyToMany)
--
-- Unicidade case-insensitive via coluna normalizada (name_normalized, minuscula
-- + trim), garantida por constraint UNIQUE — portavel entre PostgreSQL e H2
-- (evita indice funcional em lower(name), nao suportado em versoes do H2).
CREATE TABLE tags (
    id              UUID        NOT NULL PRIMARY KEY,
    name            VARCHAR(60) NOT NULL,
    name_normalized VARCHAR(60) NOT NULL,
    created_at      TIMESTAMP   NOT NULL,
    updated_at      TIMESTAMP   NOT NULL,
    CONSTRAINT uk_tags_name_normalized UNIQUE (name_normalized)
);

-- Relacao N:N card <-> tag. ON DELETE CASCADE nas duas FKs: excluir um card ou
-- uma tag remove automaticamente as linhas da juncao (detach).
CREATE TABLE card_tags (
    card_id UUID NOT NULL,
    tag_id  UUID NOT NULL,
    CONSTRAINT pk_card_tags PRIMARY KEY (card_id, tag_id),
    CONSTRAINT fk_card_tags_card FOREIGN KEY (card_id) REFERENCES cards (id) ON DELETE CASCADE,
    CONSTRAINT fk_card_tags_tag  FOREIGN KEY (tag_id)  REFERENCES tags (id)  ON DELETE CASCADE
);

CREATE INDEX idx_card_tags_tag ON card_tags (tag_id);
