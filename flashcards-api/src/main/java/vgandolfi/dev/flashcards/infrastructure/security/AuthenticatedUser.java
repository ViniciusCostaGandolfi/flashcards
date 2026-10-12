package vgandolfi.dev.flashcards.infrastructure.security;

import java.util.UUID;

/**
 * Principal autenticado, criado pelo {@link JwtAuthFilter} a partir dos claims
 * do access token (autenticacao stateless, sem consulta ao banco por requisicao).
 */
public record AuthenticatedUser(UUID userId, String email, String name) {
}
