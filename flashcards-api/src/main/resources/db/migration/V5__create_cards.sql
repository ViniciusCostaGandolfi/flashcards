-- V5: cards + opcoes (element collection ordenada)
CREATE TABLE cards (
    id               UUID         NOT NULL PRIMARY KEY,
    deck_id          UUID         NOT NULL,
    type             VARCHAR(20)  NOT NULL,
    question         TEXT         NOT NULL,
    answer           TEXT         NOT NULL,
    explanation      TEXT,
    hint             VARCHAR(500),
    position         INTEGER      NOT NULL,
    ease_factor      DOUBLE PRECISION NOT NULL DEFAULT 2.5,
    interval_days    INTEGER      NOT NULL DEFAULT 0,
    repetitions      INTEGER      NOT NULL DEFAULT 0,
    due_date         DATE         NOT NULL,
    last_reviewed_at TIMESTAMP,
    created_at       TIMESTAMP    NOT NULL,
    updated_at       TIMESTAMP    NOT NULL,
    CONSTRAINT fk_cards_deck FOREIGN KEY (deck_id) REFERENCES decks (id) ON DELETE CASCADE
);

CREATE INDEX idx_cards_deck_id ON cards (deck_id);
CREATE INDEX idx_cards_due_date ON cards (due_date);

CREATE TABLE card_options (
    card_id      UUID    NOT NULL,
    option_index INTEGER NOT NULL,
    option_value TEXT,
    CONSTRAINT pk_card_options PRIMARY KEY (card_id, option_index),
    CONSTRAINT fk_card_options_card FOREIGN KEY (card_id) REFERENCES cards (id) ON DELETE CASCADE
);
