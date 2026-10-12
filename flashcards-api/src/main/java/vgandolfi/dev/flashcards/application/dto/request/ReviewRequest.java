package vgandolfi.dev.flashcards.application.dto.request;

import jakarta.validation.constraints.NotNull;
import vgandolfi.dev.flashcards.domain.service.ReviewGrade;

public record ReviewRequest(@NotNull ReviewGrade grade) {
}
