package vgandolfi.dev.flashcards;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Ponto de entrada da API de Flashcards.
 *
 * <p>Habilita {@code @ConfigurationPropertiesScan} para registrar os records
 * {@code AppProperties} ({@code app.*}), {@code OpenRouterProperties}
 * ({@code app.openrouter.*}) e {@code S3Properties} ({@code app.s3.*}).</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan("vgandolfi.dev.flashcards")
public class FlashcardsApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlashcardsApplication.class, args);
    }
}
