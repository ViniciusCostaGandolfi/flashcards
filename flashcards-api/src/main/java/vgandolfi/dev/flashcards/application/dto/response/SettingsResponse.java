package vgandolfi.dev.flashcards.application.dto.response;

import vgandolfi.dev.flashcards.domain.model.AppSettings;

public record SettingsResponse(
        String readerModel,
        String generatorModel,
        String basePrompt,
        int cardsPerGeneration,
        String language,
        String pdfEngine) {

    public static SettingsResponse from(AppSettings settings) {
        return new SettingsResponse(
                settings.getReaderModel(),
                settings.getGeneratorModel(),
                settings.getBasePrompt(),
                settings.getCardsPerGeneration(),
                settings.getLanguage(),
                settings.getPdfEngine());
    }
}
