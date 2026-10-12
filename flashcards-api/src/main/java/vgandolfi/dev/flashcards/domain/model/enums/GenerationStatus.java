package vgandolfi.dev.flashcards.domain.model.enums;

/** Ciclo de vida de uma {@code GenerationJob}. */
public enum GenerationStatus {
    PENDING,
    READING,
    GENERATING,
    DONE,
    FAILED
}
