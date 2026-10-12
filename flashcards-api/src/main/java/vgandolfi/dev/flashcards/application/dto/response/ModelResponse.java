package vgandolfi.dev.flashcards.application.dto.response;

import java.util.List;

/**
 * Modelo disponivel no catalogo curado.
 *
 * @param id              identificador no OpenRouter (ex.: {@code google/gemini-2.5-flash})
 * @param name            nome amigavel
 * @param inputModalities modalidades de entrada aceitas (ex.: {@code text}, {@code image}, {@code file})
 */
public record ModelResponse(String id, String name, List<String> inputModalities) {
}
