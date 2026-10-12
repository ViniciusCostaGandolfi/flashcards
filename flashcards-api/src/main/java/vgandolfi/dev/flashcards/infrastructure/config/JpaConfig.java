package vgandolfi.dev.flashcards.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Habilita a auditoria JPA (preenchimento automatico de
 * {@code createdAt}/{@code updatedAt} via {@code AuditingEntityListener}).
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
