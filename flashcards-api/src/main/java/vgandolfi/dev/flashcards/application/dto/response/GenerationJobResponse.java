package vgandolfi.dev.flashcards.application.dto.response;

import vgandolfi.dev.flashcards.domain.model.GenerationJob;
import vgandolfi.dev.flashcards.domain.model.enums.GenerationStatus;

import java.util.UUID;

/**
 * Status de um {@code GenerationJob} para polling.
 *
 * @param cardsCount quantidade de cards do deck gerado (0 enquanto nao concluido)
 */
public record GenerationJobResponse(
        UUID id,
        GenerationStatus status,
        String errorMessage,
        UUID deckId,
        long cardsCount) {

    public static GenerationJobResponse from(GenerationJob job, long cardsCount) {
        return new GenerationJobResponse(
                job.getId(),
                job.getStatus(),
                job.getErrorMessage(),
                job.getDeck() == null ? null : job.getDeck().getId(),
                cardsCount);
    }
}
