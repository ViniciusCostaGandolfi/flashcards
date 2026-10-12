package vgandolfi.dev.flashcards.infrastructure.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import vgandolfi.dev.flashcards.infrastructure.openrouter.OpenRouterProperties;

/**
 * Cliente HTTP para o OpenRouter via Spring {@link RestClient}.
 *
 * <p>URL base vem de {@code app.openrouter.url}. Timeouts generosos (10s
 * connect / 120s read) porque a geracao com modelos de raciocinio pode demorar.
 * O header {@code Authorization: Bearer <key>} e adicionado por chamada.</p>
 */
@Configuration
public class OpenRouterRestClientConfig {

    @Bean
    public RestClient openRouterRestClient(OpenRouterProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(120));

        RestClient.Builder builder = RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        if (properties.url() != null && !properties.url().isBlank()) {
            builder.baseUrl(properties.url());
        }
        return builder.build();
    }
}
