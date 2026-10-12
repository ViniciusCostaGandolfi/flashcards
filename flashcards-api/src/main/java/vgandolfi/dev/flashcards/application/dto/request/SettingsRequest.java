package vgandolfi.dev.flashcards.application.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Atualizacao das configuracoes globais. Campos nulos mantem o valor atual
 * (o servico aplica apenas o que vier preenchido).
 */
public record SettingsRequest(
        @Size(max = 120) String readerModel,
        @Size(max = 120) String generatorModel,
        String basePrompt,
        @Min(3) Integer cardsPerGeneration,
        @Size(max = 20) String language,
        @Size(max = 30) String pdfEngine) {
}
