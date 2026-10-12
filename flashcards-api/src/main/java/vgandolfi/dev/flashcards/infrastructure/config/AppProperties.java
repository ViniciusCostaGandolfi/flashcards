package vgandolfi.dev.flashcards.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades customizadas da aplicacao (prefixo {@code app.}).
 *
 * <p>Segredos ({@code jwt.secret}, {@code user.password}) vem de variaveis de
 * ambiente. O perfil dev tem placeholders genericos; o perfil prod usa
 * placeholders obrigatorios (sem default) — ver {@code application-prod.yaml}.</p>
 *
 * <p>As propriedades de OpenRouter ({@code app.openrouter.*}) e de object
 * storage ({@code app.s3.*}) tem seus proprios records
 * ({@link vgandolfi.dev.flashcards.infrastructure.openrouter.OpenRouterProperties}
 * e {@link vgandolfi.dev.flashcards.infrastructure.config.properties.S3Properties}).</p>
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, User user, Frontend frontend) {

    public record Jwt(String secret, long expiration, long refreshExpiration) {
    }

    /** Usuario unico semeado no boot ({@code UserInitializer}). */
    public record User(String email, String password) {
    }

    public record Frontend(String url) {
    }
}
