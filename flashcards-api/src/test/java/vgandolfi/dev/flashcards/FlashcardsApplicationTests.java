package vgandolfi.dev.flashcards;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test de contexto completo: valida Flyway (migrations) + Hibernate
 * {@code ddl-auto=validate} + wiring dos beans no perfil {@code test} (H2).
 */
@SpringBootTest
class FlashcardsApplicationTests {

    @Test
    void contextLoads() {
    }
}
