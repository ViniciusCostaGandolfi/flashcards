package vgandolfi.dev.flashcards.application.dto.response;

import vgandolfi.dev.flashcards.domain.model.Deck;
import vgandolfi.dev.flashcards.domain.model.enums.SourceType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Detalhe de um deck ({@code GET /api/v1/decks/{id}}). */
public record DeckResponse(
        UUID id,
        String title,
        SourceType sourceType,
        UUID sourceFileId,
        String extractedText,
        String readerModel,
        String generatorModel,
        String promptUsed,
        long cardsCount,
        long dueCount,
        List<String> tags,
        Instant createdAt) {

    public static DeckResponse from(Deck deck, long cardsCount, long dueCount, List<String> tags) {
        return new DeckResponse(
                deck.getId(),
                deck.getTitle(),
                deck.getSourceType(),
                deck.getSourceFile() == null ? null : deck.getSourceFile().getId(),
                deck.getExtractedText(),
                deck.getReaderModel(),
                deck.getGeneratorModel(),
                deck.getPromptUsed(),
                cardsCount,
                dueCount,
                tags == null ? List.of() : tags,
                deck.getCreatedAt());
    }
}
