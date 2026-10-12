package vgandolfi.dev.flashcards.application.dto.response;

import vgandolfi.dev.flashcards.domain.model.Deck;
import vgandolfi.dev.flashcards.domain.model.enums.SourceType;

import java.time.Instant;
import java.util.UUID;

/** Resumo de deck para a listagem ({@code GET /api/v1/decks}). */
public record DeckSummaryResponse(
        UUID id,
        String title,
        SourceType sourceType,
        long cardsCount,
        long dueCount,
        Instant createdAt) {

    public static DeckSummaryResponse from(Deck deck, long cardsCount, long dueCount) {
        return new DeckSummaryResponse(
                deck.getId(),
                deck.getTitle(),
                deck.getSourceType(),
                cardsCount,
                dueCount,
                deck.getCreatedAt());
    }
}
