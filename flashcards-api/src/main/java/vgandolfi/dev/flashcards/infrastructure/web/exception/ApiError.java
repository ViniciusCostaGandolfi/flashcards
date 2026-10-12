package vgandolfi.dev.flashcards.infrastructure.web.exception;

import java.time.Instant;
import java.util.List;

/**
 * Corpo padrao de erro das respostas HTTP da API:
 * {@code {status, message, timestamp, errors[]}}.
 */
public record ApiError(int status, String message, Instant timestamp, List<String> errors) {

    public static ApiError of(int status, String message) {
        return new ApiError(status, message, Instant.now(), null);
    }

    public static ApiError withErrors(int status, String message, List<String> errors) {
        return new ApiError(status, message, Instant.now(), errors);
    }
}
