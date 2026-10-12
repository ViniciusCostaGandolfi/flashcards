package vgandolfi.dev.flashcards.application.dto.response;

import vgandolfi.dev.flashcards.domain.model.Card;
import vgandolfi.dev.flashcards.domain.model.Tag;
import vgandolfi.dev.flashcards.domain.model.enums.CardType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record CardResponse(
        UUID id,
        UUID deckId,
        CardType type,
        String question,
        String answer,
        String explanation,
        String hint,
        int position,
        List<String> options,
        List<String> tags,
        double easeFactor,
        int intervalDays,
        int repetitions,
        LocalDate dueDate,
        Instant lastReviewedAt) {

    public static CardResponse from(Card card) {
        return new CardResponse(
                card.getId(),
                card.getDeck() == null ? null : card.getDeck().getId(),
                card.getType(),
                card.getQuestion(),
                card.getAnswer(),
                card.getExplanation(),
                card.getHint(),
                card.getPosition(),
                List.copyOf(card.getOptions()),
                tagNames(card),
                card.getEaseFactor(),
                card.getIntervalDays(),
                card.getRepetitions(),
                card.getDueDate(),
                card.getLastReviewedAt());
    }

    private static List<String> tagNames(Card card) {
        return card.getTags().stream()
                .map(Tag::getName)
                .sorted(Comparator.comparing(String::toLowerCase))
                .toList();
    }
}
