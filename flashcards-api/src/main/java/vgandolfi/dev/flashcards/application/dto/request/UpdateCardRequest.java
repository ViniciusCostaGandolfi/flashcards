package vgandolfi.dev.flashcards.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import vgandolfi.dev.flashcards.domain.model.enums.CardType;

import java.util.List;

public record UpdateCardRequest(
        @NotNull CardType type,
        @NotBlank String question,
        @NotBlank String answer,
        String explanation,
        @Size(max = 500) String hint,
        @PositiveOrZero Integer position,
        List<String> options,
        List<String> tags) {
}
