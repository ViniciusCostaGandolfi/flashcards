package vgandolfi.dev.flashcards;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Valida as migrations Flyway contra um PostgreSQL real (Testcontainers) e o
 * {@code ddl-auto=validate} do Hibernate.
 *
 * <p>Auto-desabilitado quando o Docker nao esta disponivel
 * ({@code disabledWithoutDocker = true}) — os testes unitarios e de web slice
 * continuam verdes sem Docker.</p>
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PostgresMigrationIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Test
    void migrationsApplyAndSchemaValidates() {
        assertThat(POSTGRES.isRunning()).isTrue();
    }
}
