package vgandolfi.dev.flashcards.infrastructure.openrouter;

import java.util.List;

/**
 * Cartao gerado pelo modelo (antes de virar entidade {@code Card}). {@code type}
 * fica como String (ex.: {@code CLOZE}) e e convertido defensivamente pelo
 * {@code GenerationService}.
 *
 * <p>{@code tags} e opcional: quando ausente no JSON do modelo, vem vazio
 * (parsing tolerante/retrocompativel).</p>
 */
public record GeneratedCard(
        String type,
        String question,
        List<String> options,
        String answer,
        String explanation,
        List<String> tags) {
}
