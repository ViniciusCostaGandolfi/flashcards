package vgandolfi.dev.flashcards.domain.exception;

/**
 * Conflito de estado (mapeada para HTTP 409 pelo {@code GlobalExceptionHandler}).
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
