package vgandolfi.dev.flashcards.infrastructure.openrouter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracao do cliente OpenRouter ({@code app.openrouter.*}).
 *
 * <p>Quando {@link #apiKey} esta em branco, o {@code MockOpenRouterClient} e
 * registrado em vez do cliente real (ver {@code RealOpenRouterClient} /
 * {@code MockOpenRouterClient}) — garante exatamente um bean por contexto.</p>
 */
@ConfigurationProperties(prefix = "app.openrouter")
public record OpenRouterProperties(
        String url,
        String apiKey,
        String readerModel,
        String generatorModel,
        String basePrompt,
        String referer,
        String title) {
}
