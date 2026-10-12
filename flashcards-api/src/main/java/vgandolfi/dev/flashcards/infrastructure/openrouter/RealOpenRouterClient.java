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

import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cliente real do OpenRouter. Registrado apenas quando
 * {@code app.openrouter.api-key} esta preenchido.
 */
@Component
@ConditionalOnExpression("'${app.openrouter.api-key:}' != ''")
public class RealOpenRouterClient implements OpenRouterClient {

    private static final Logger log = LoggerFactory.getLogger(RealOpenRouterClient.class);

    /**
     * Mapper tolerante para o JSON (nem sempre estrito) devolvido pelos modelos:
     * aceita chaves sem aspas, aspas simples, virgulas finais e comentarios.
     * Nao substitui o mapper global da aplicacao.
     */
    private static final ObjectMapper LENIENT_MAPPER = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_TRAILING_COMMA,
                    JsonReadFeature.ALLOW_SINGLE_QUOTES,
                    JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES,
                    JsonReadFeature.ALLOW_JAVA_COMMENTS,
                    JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS,
                    JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
            .build();

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

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", List.of(message));

        // Pede JSON ao provedor (aumenta muito a aderencia). Se o modelo nao
        // suportar response_format, cai para a chamada simples.
        Map<String, Object> jsonBody = new HashMap<>(body);
        jsonBody.put("response_format", Map.of("type", "json_object"));

        JsonNode response;
        try {
            response = chat(jsonBody);
        } catch (RuntimeException ex) {
            log.warn("openrouter_json_mode_failed_retrying_plain error={}", ex.getMessage());
            response = chat(body);
        }
        return parseCards(contentOf(response));
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

    /** Parse defensivo de {@code {"cards":[...]}} tolerando ruido, cercas e JSON frouxo. */
    List<GeneratedCard> parseCards(String raw) {
        String json = extractJson(raw);
        List<GeneratedCard> cards = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return cards;
        }
        try {
            JsonNode root = LENIENT_MAPPER.readTree(json);
            JsonNode array = root.path("cards");
            if (!array.isArray()) {
                array = root.path("flashcards");
            }
            if (!array.isArray() && root.isArray()) {
                array = root;
            }
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

    /**
     * Extrai o primeiro objeto/array JSON balanceado, tolerando cercas de codigo
     * (com ou sem linguagem), BOM e texto antes/depois. Varredura por
     * profundidade que respeita strings (aspas duplas ou simples) e escapes.
     */
    private String extractJson(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.strip();
        if (!s.isEmpty() && s.charAt(0) == '\uFEFF') {
            s = s.substring(1).strip();
        }
        int start = -1;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '[') {
                start = i;
                break;
            }
        }
        if (start < 0) {
            return s;
        }
        int depth = 0;
        char quote = 0;
        boolean escape = false;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (quote != 0) {
                if (escape) {
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == quote) {
                    quote = 0;
                }
                continue;
            }
            if (c == '"' || c == '\'') {
                quote = c;
                continue;
            }
            if (c == '{' || c == '[') {
                depth++;
            } else if (c == '}' || c == ']') {
                depth--;
                if (depth == 0) {
                    return s.substring(start, i + 1);
                }
            }
        }
        return s.substring(start);
    }
}
