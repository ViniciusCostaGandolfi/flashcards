package vgandolfi.dev.flashcards.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuracao do object storage compativel com S3/MinIO ({@code app.s3.*}). */
@ConfigurationProperties(prefix = "app.s3")
public record S3Properties(String endpoint, String accessKey, String secretKey, String bucket, String region) {
}
