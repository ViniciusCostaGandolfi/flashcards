package vgandolfi.dev.flashcards.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vgandolfi.dev.flashcards.application.dto.request.SettingsRequest;
import vgandolfi.dev.flashcards.application.dto.response.SettingsResponse;
import vgandolfi.dev.flashcards.domain.exception.EntityNotFoundException;
import vgandolfi.dev.flashcards.domain.model.AppSettings;
import vgandolfi.dev.flashcards.domain.repository.AppSettingsRepository;

/**
 * Leitura/atualizacao das configuracoes globais (linha singleton). A criacao da
 * linha com defaults e feita no boot por
 * {@code vgandolfi.dev.flashcards.infrastructure.seed.SettingsInitializer}.
 */
@Service
public class SettingsService {

    private static final Logger log = LoggerFactory.getLogger(SettingsService.class);

    private final AppSettingsRepository appSettingsRepository;

    public SettingsService(AppSettingsRepository appSettingsRepository) {
        this.appSettingsRepository = appSettingsRepository;
    }

    @Transactional(readOnly = true)
    public SettingsResponse get() {
        return SettingsResponse.from(requireSettings());
    }

    /** Entidade atual; usada internamente pela pipeline de geracao. */
    @Transactional(readOnly = true)
    public AppSettings requireSettings() {
        return appSettingsRepository.findFirstByOrderByCreatedAtAsc()
                .orElseThrow(() -> new EntityNotFoundException("Configuracoes nao inicializadas"));
    }

    @Transactional
    public SettingsResponse update(SettingsRequest request) {
        AppSettings settings = requireSettings();
        if (request.readerModel() != null) {
            settings.setReaderModel(request.readerModel());
        }
        if (request.generatorModel() != null) {
            settings.setGeneratorModel(request.generatorModel());
        }
        if (request.basePrompt() != null) {
            settings.setBasePrompt(request.basePrompt());
        }
        if (request.cardsPerGeneration() != null) {
            settings.setCardsPerGeneration(Math.max(AppSettings.MIN_CARDS_PER_GENERATION,
                    request.cardsPerGeneration()));
        }
        if (request.language() != null) {
            settings.setLanguage(request.language());
        }
        if (request.pdfEngine() != null) {
            settings.setPdfEngine(request.pdfEngine());
        }
        AppSettings saved = appSettingsRepository.save(settings);
        log.info("settings_updated cardsPerGeneration={} language={}", saved.getCardsPerGeneration(),
                saved.getLanguage());
        return SettingsResponse.from(saved);
    }
}
