package vgandolfi.dev.flashcards.domain.exception;

/**
 * Recurso inexistente (mapeada para HTTP 404 pelo {@code GlobalExceptionHandler}).
 */
public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String message) {
        super(message);
    }
}
