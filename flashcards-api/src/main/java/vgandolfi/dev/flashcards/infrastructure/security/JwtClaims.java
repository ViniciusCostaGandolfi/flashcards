package vgandolfi.dev.flashcards.infrastructure.security;

import java.time.Instant;
import java.util.UUID;

/** Claims extraidos de um access token JWT valido. */
public record JwtClaims(UUID userId, String email, String name, Instant expiresAt) {
}
