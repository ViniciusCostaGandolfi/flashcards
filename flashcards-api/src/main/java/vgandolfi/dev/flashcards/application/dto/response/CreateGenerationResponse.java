package vgandolfi.dev.flashcards.application.dto.response;

import java.util.UUID;

/** Resposta 202 do {@code POST /api/v1/generations}. */
public record CreateGenerationResponse(UUID jobId) {
}
