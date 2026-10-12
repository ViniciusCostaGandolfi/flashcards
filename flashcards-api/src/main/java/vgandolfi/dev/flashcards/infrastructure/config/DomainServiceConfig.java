package vgandolfi.dev.flashcards.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vgandolfi.dev.flashcards.domain.service.SrsScheduler;

/**
 * Registra os servicos de dominio (sem dependencia de framework) como beans,
 * para injecao por construtor nos servicos de aplicacao.
 */
@Configuration
public class DomainServiceConfig {

    @Bean
    public SrsScheduler srsScheduler() {
        return new SrsScheduler();
    }
}
