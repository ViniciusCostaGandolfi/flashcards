package vgandolfi.dev.flashcards.application.event;

import java.util.List;
import java.util.UUID;

/**
 * Publicado (dentro da transacao de criacao) quando um {@code GenerationJob} e
 * persistido. Consumido de forma assincrona apos o commit, para disparar a
 * pipeline de geracao.
 *
 * @param jobId id do job
 * @param tags  tags (nomes) a aplicar em TODOS os cards gerados
 */
public record GenerationJobCreatedEvent(UUID jobId, List<String> tags) {
}
