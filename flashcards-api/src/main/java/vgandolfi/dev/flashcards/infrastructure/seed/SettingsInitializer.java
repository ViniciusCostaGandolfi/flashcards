package vgandolfi.dev.flashcards.infrastructure.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vgandolfi.dev.flashcards.domain.model.AppSettings;
import vgandolfi.dev.flashcards.domain.repository.AppSettingsRepository;
import vgandolfi.dev.flashcards.infrastructure.openrouter.OpenRouterProperties;

/**
 * Garante a linha singleton de {@link AppSettings}, aplicando como default os
 * modelos/prompt vindos de {@code app.openrouter.*}. Idempotente.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class SettingsInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SettingsInitializer.class);

    private final AppSettingsRepository appSettingsRepository;
    private final OpenRouterProperties openRouterProperties;

    public SettingsInitializer(AppSettingsRepository appSettingsRepository,
                               OpenRouterProperties openRouterProperties) {
        this.appSettingsRepository = appSettingsRepository;
        this.openRouterProperties = openRouterProperties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (appSettingsRepository.findFirstByOrderByCreatedAtAsc().isPresent()) {
            log.info("settings_initializer_skip reason=already_exists");
            return;
        }
        AppSettings settings = new AppSettings();
        settings.setReaderModel(openRouterProperties.readerModel());
        settings.setGeneratorModel(openRouterProperties.generatorModel());
        settings.setBasePrompt(openRouterProperties.basePrompt());
        settings.setCardsPerGeneration(AppSettings.DEFAULT_CARDS_PER_GENERATION);
        settings.setLanguage(AppSettings.DEFAULT_LANGUAGE);
        settings.setPdfEngine(AppSettings.DEFAULT_PDF_ENGINE);
        appSettingsRepository.save(settings);
        log.info("settings_initializer_created readerModel={} generatorModel={}",
                settings.getReaderModel(), settings.getGeneratorModel());
    }
}
