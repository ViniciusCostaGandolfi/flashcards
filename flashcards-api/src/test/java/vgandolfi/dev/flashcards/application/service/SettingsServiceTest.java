package vgandolfi.dev.flashcards.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vgandolfi.dev.flashcards.application.dto.request.SettingsRequest;
import vgandolfi.dev.flashcards.application.dto.response.SettingsResponse;
import vgandolfi.dev.flashcards.domain.exception.EntityNotFoundException;
import vgandolfi.dev.flashcards.domain.model.AppSettings;
import vgandolfi.dev.flashcards.domain.repository.AppSettingsRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

    @Mock
    private AppSettingsRepository appSettingsRepository;

    @InjectMocks
    private SettingsService settingsService;

    private AppSettings settings() {
        AppSettings settings = new AppSettings();
        settings.setReaderModel("reader");
        settings.setGeneratorModel("generator");
        settings.setBasePrompt("prompt");
        settings.setCardsPerGeneration(6);
        settings.setLanguage("pt-BR");
        settings.setPdfEngine("native");
        return settings;
    }

    @Test
    void getReturnsCurrentSettings() {
        when(appSettingsRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(settings()));

        SettingsResponse response = settingsService.get();

        assertThat(response.readerModel()).isEqualTo("reader");
        assertThat(response.cardsPerGeneration()).isEqualTo(6);
        assertThat(response.language()).isEqualTo("pt-BR");
    }

    @Test
    void getWithoutRowThrows() {
        when(appSettingsRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> settingsService.get()).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateAppliesProvidedFieldsOnly() {
        AppSettings current = settings();
        when(appSettingsRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(current));
        when(appSettingsRepository.save(any(AppSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        SettingsResponse response = settingsService.update(
                new SettingsRequest(null, "novo-generator", null, 10, null, "pdfium"));

        assertThat(response.readerModel()).isEqualTo("reader");
        assertThat(response.generatorModel()).isEqualTo("novo-generator");
        assertThat(response.cardsPerGeneration()).isEqualTo(10);
        assertThat(response.pdfEngine()).isEqualTo("pdfium");
    }

    @Test
    void updateClampsCardsPerGenerationToMinimum() {
        when(appSettingsRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(settings()));
        when(appSettingsRepository.save(any(AppSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        SettingsResponse response = settingsService.update(
                new SettingsRequest(null, null, null, 1, null, null));

        assertThat(response.cardsPerGeneration()).isEqualTo(AppSettings.MIN_CARDS_PER_GENERATION);
    }
}
