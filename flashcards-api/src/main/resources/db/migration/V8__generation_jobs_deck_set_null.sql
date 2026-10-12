-- V8: permite excluir um deck sem violar a FK dos jobs de geracao.
-- Ao excluir o deck, os jobs que o referenciam ficam com deck_id = NULL
-- (preserva o historico do job). Antes o ON DELETE era NO ACTION e o delete
-- do deck estourava ConstraintViolationException (fk_generation_jobs_deck).
ALTER TABLE generation_jobs DROP CONSTRAINT fk_generation_jobs_deck;
ALTER TABLE generation_jobs ADD CONSTRAINT fk_generation_jobs_deck
    FOREIGN KEY (deck_id) REFERENCES decks (id) ON DELETE SET NULL;
