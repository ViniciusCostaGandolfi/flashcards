package vgandolfi.dev.flashcards.infrastructure.openrouter;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Cliente mock do OpenRouter (dev/test): registrado quando
 * {@code app.openrouter.api-key} esta em branco. Retorna uma extracao fixa e
 * exatamente 3 cards, um de cada tipo — permitindo exercitar toda a pipeline
 * sem dependencia de rede.
 */
@Component
@ConditionalOnExpression("'${app.openrouter.api-key:}' == ''")
public class MockOpenRouterClient implements OpenRouterClient {

    static final String CANNED_TEXT =
            "Material de exemplo (mock). A fotossintese converte luz solar, agua e gas carbonico "
                    + "em glicose e oxigenio. A mitocondria e a organela responsavel pela respiracao "
                    + "celular. A agua e uma molecula polar.";

    @Override
    public String extractText(String model, byte[] data, String contentType, String filename) {
        return CANNED_TEXT;
    }

    @Override
    public List<GeneratedCard> generateCards(String model, String basePrompt, String text, int minCards) {
        return List.of(
                new GeneratedCard(
                        "CLOZE",
                        "A {{c1::fotossintese}} converte luz solar, agua e gas carbonico em glicose e oxigenio.",
                        List.of(),
                        "fotossintese",
                        "Processo realizado por plantas e algas.",
                        List.of("biologia")),
                new GeneratedCard(
                        "TRUE_FALSE",
                        "A mitocondria e a organela responsavel pela respiracao celular.",
                        List.of(),
                        "Verdadeiro",
                        "A mitocondria produz ATP via respiracao celular.",
                        List.of("biologia")),
                new GeneratedCard(
                        "MULTIPLE_CHOICE",
                        "Qual e a propriedade da agua descrita no material?",
                        List.of("Molecula apolar", "Molecula polar", "Nao conduz eletricidade", "E um gas"),
                        "Molecula polar",
                        "A agua e uma molecula polar devido a diferenca de eletronegatividade.",
                        List.of("quimica")));
    }
}
