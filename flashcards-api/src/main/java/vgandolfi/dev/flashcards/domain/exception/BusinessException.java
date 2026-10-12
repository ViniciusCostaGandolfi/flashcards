package vgandolfi.dev.flashcards.domain.exception;

/**
 * Excecao de regra de negocio (mapeada para HTTP 400 pelo
 * {@code GlobalExceptionHandler}).
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
