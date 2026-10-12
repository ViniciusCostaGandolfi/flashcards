package vgandolfi.dev.flashcards.infrastructure.openrouter;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Cliente real do OpenRouter. Registrado apenas quando
 * {@code app.openrouter.api-key} esta preenchido.
 */
@Component
@ConditionalOnExpression("'${app.openrouter.api-key:}' != ''")
public class RealOpenRouterClient implements OpenRouterClient {

    private static final Logger log = LoggerFactory.getLogger(RealOpenRouterClient.class);

    private static final String EXTRACTION_INSTRUCTION =
            "Transcreva fielmente TODO o texto contido no material a seguir. "
                    + "Preserve tabelas, listas e formulas. Responda apenas com o texto extraido, "
                    + "sem comentarios, sem marcacao e sem delimitadores de codigo.";

    private final RestClient restClient;
    private final OpenRouterProperties properties;
    private final ObjectMapper objectMapper;

    public RealOpenRouterClient(@Qualifier("openRouterRestClient") RestClient restClient,
                                OpenRouterProperties properties,
                                ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public String extractText(String model, byte[] data, String contentType, String filename) {
        String base64 = Base64.getEncoder().encodeToString(data);
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(Map.of("type", "text", "text", EXTRACTION_INSTRUCTION));

        if (contentType != null && contentType.startsWith("image/")) {
            content.add(Map.of("type", "image_url",
                    "image_url", Map.of("url", "data:" + contentType + ";base64," + base64)));
        } else {
            content.add(Map.of("type", "file",
                    "file", Map.of(
                            "filename", filename == null ? "document.pdf" : filename,
                            "file_data", "data:application/pdf;base64," + base64)));
        }

        Map<String, Object> message = Map.of("role", "user", "content", content);
        JsonNode response = chat(Map.of("model", model, "messages", List.of(message)));
        return contentOf(response);
    }

    @Override
    public List<GeneratedCard> generateCards(String model, String basePrompt, String text, int minCards) {
        String prompt = basePrompt
                + "\n\nGere ao menos " + Math.max(minCards, 1) + " flashcards.\n\n"
                + "MATERIAL:\n" + (text == null ? "" : text);

        Map<String, Object> message = Map.of("role", "user", "content", prompt);
        JsonNode response = chat(Map.of(
                "model", model,
                "messages", List.of(message)));
        String raw = contentOf(response);
        return parseCards(raw);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private JsonNode chat(Map<String, Object> body) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey());
        if (properties.referer() != null && !properties.referer().isBlank()) {
            headers.put("HTTP-Referer", properties.referer());
        }
        if (properties.title() != null && !properties.title().isBlank()) {
            headers.put("X-Title", properties.title());
        }
        try {
            JsonNode response = restClient.post()
                    .uri("/chat/completions")
                    .headers(h -> headers.forEach((k, v) -> h.set(k, String.valueOf(v))))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) {
                throw new IllegalStateException("Resposta vazia do OpenRouter");
            }
            return response;
        } catch (RestClientException ex) {
            log.error("openrouter_request_failed", ex);
            throw new IllegalStateException("Falha ao chamar o OpenRouter: " + ex.getMessage(), ex);
        }
    }

    /** Extrai {@code choices[0].message.content}. */
    private String contentOf(JsonNode response) {
        JsonNode content = response.path("choices").path(0).path("message").path("content");
        if (content.isTextual()) {
            return content.asText();
        }
        if (content.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode part : content) {
                JsonNode text = part.path("text");
                if (text.isTextual()) {
                    sb.append(text.asText());
                }
            }
            return sb.toString();
        }
        throw new IllegalStateException("Resposta do OpenRouter sem conteudo textual");
    }

    /** Parse defensivo de {@code {"cards":[...]}} tolerando cercas ```json. */
    List<GeneratedCard> parseCards(String raw) {
        String cleaned = stripFences(raw);
        List<GeneratedCard> cards = new ArrayList<>();
        if (cleaned == null || cleaned.isBlank()) {
            return cards;
        }
        try {
            JsonNode root = objectMapper.readTree(cleaned);
            JsonNode array = root.path("cards");
            if (!array.isArray()) {
                return cards;
            }
            for (JsonNode node : array) {
                cards.add(new GeneratedCard(
                        text(node, "type"),
                        text(node, "question"),
                        stringList(node, "options"),
                        text(node, "answer"),
                        text(node, "explanation"),
                        stringList(node, "tags")));
            }
        } catch (RuntimeException ex) {
            log.warn("openrouter_cards_parse_failed error={}", ex.getMessage());
        }
        return cards;
    }

    /** Extrai um array de strings de um campo, tolerando ausencia/tipo invalido. */
    private List<String> stringList(JsonNode node, String field) {
        JsonNode array = node.path(field);
        List<String> result = new ArrayList<>();
        if (array.isArray()) {
            for (JsonNode item : array) {
                if (item.isTextual() && !item.asText().isBlank()) {
                    result.add(item.asText());
                }
            }
        }
        return result;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() ? value.asText() : null;
    }

    /** Remove cercas de codigo e ruido ao redor do JSON. */
    private String stripFences(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = raw.trim();
        if (cleaned.startsWith("```")) {
            int newline = cleaned.indexOf('\n');
            cleaned = newline >= 0 ? cleaned.substring(newline + 1) : cleaned.substring(3);
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
            cleaned = cleaned.trim();
        }
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            cleaned = cleaned.substring(start, end + 1);
        }
        return cleaned;
    }
}
