package vgandolfi.dev.flashcards.infrastructure.openrouter;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobre o parse defensivo de cards do OpenRouter (cercas de codigo, ruido ao
 * redor do JSON e JSON invalido).
 */
class OpenRouterJsonParsingTest {

    private final RealOpenRouterClient client =
            new RealOpenRouterClient(null, null, JsonMapper.builder().build());

    @Test
    void parsesCardsFromPlainJson() {
        String raw = "{\"cards\":[{\"type\":\"CLOZE\",\"question\":\"A {{c1::mitocondria}} produz ATP\","
                + "\"options\":[],\"answer\":\"mitocondria\",\"explanation\":\"organela\"}]}";

        List<GeneratedCard> cards = client.parseCards(raw);

        assertThat(cards).hasSize(1);
        assertThat(cards.get(0).type()).isEqualTo("CLOZE");
        assertThat(cards.get(0).answer()).isEqualTo("mitocondria");
        assertThat(cards.get(0).explanation()).isEqualTo("organela");
    }

    @Test
    void parsesCardsWrappedInCodeFences() {
        String raw = """
                ```json
                {"cards":[{"type":"TRUE_FALSE","question":"A agua e polar?","options":[],"answer":"Verdadeiro","explanation":null}]}
                ```
                """;

        List<GeneratedCard> cards = client.parseCards(raw);

        assertThat(cards).hasSize(1);
        assertThat(cards.get(0).type()).isEqualTo("TRUE_FALSE");
    }

    @Test
    void parsesCardsFromJsonSurroundedByNoise() {
        String raw = "Claro! Aqui estao os cards:\n"
                + "{\"cards\":[{\"type\":\"MULTIPLE_CHOICE\",\"question\":\"Qual?\","
                + "\"options\":[\"A\",\"B\",\"C\",\"D\"],\"answer\":\"B\",\"explanation\":\"porque\"}]}\n"
                + "Espero ter ajudado.";

        List<GeneratedCard> cards = client.parseCards(raw);

        assertThat(cards).hasSize(1);
        assertThat(cards.get(0).options()).containsExactly("A", "B", "C", "D");
    }

    @Test
    void parsesOptionalTagsPerCard() {
        String raw = "{\"cards\":[{\"type\":\"CLOZE\",\"question\":\"q\",\"options\":[],\"answer\":\"a\","
                + "\"explanation\":null,\"tags\":[\"Biologia\",\"  \",\"Citologia\"]}]}";

        List<GeneratedCard> cards = client.parseCards(raw);

        assertThat(cards).hasSize(1);
        assertThat(cards.get(0).tags()).containsExactly("Biologia", "Citologia");
    }

    @Test
    void cardsWithoutTagsYieldEmptyTags() {
        String raw = "{\"cards\":[{\"type\":\"CLOZE\",\"question\":\"q\",\"options\":[],\"answer\":\"a\"}]}";

        assertThat(client.parseCards(raw).get(0).tags()).isEmpty();
    }

    @Test
    void returnsEmptyListOnMalformedJson() {
        assertThat(client.parseCards("isto nao e json")).isEmpty();
        assertThat(client.parseCards("{\"cards\": []}")).isEmpty();
        assertThat(client.parseCards(null)).isEmpty();
        assertThat(client.parseCards("")).isEmpty();
    }
}
