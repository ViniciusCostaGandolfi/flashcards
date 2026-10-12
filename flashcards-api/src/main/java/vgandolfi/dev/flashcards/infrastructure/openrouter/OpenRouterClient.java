package vgandolfi.dev.flashcards.infrastructure.openrouter;

import java.util.List;

/**
 * Porta de integracao com o OpenRouter (ou mock). Exatamente um bean e
 * registrado por contexto: {@link RealOpenRouterClient} quando
 * {@code app.openrouter.api-key} esta preenchido, {@link MockOpenRouterClient}
 * caso contrario.
 */
public interface OpenRouterClient {

    /**
     * Extrai/transcreve todo o texto de uma imagem ou PDF.
     *
     * @param model       modelo leitor (ex.: {@code google/gemini-2.5-flash})
     * @param data        bytes do arquivo
     * @param contentType MIME type do arquivo
     * @param filename    nome original do arquivo
     * @return texto extraido
     */
    String extractText(String model, byte[] data, String contentType, String filename);

    /**
     * Gera cards a partir de um texto, respeitando o prompt base.
     *
     * @param model      modelo gerador
     * @param basePrompt prompt base (idioma, tipos, formato JSON)
     * @param text       material de origem
     * @param minCards   numero minimo de cards desejado
     * @return cards gerados (pode vir incompleto; o servico valida/conserta)
     */
    List<GeneratedCard> generateCards(String model, String basePrompt, String text, int minCards);
}
