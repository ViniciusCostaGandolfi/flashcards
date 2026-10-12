package vgandolfi.dev.flashcards.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Habilita {@code @Async} e define o executor dedicado a pipeline de geracao
 * de flashcards (chamadas longas a OpenRouter).
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "generationExecutor")
    public Executor generationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("generation-");
        executor.initialize();
        return executor;
    }
}
