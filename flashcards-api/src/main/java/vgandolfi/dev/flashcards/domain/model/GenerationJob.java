package vgandolfi.dev.flashcards.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vgandolfi.dev.flashcards.domain.model.enums.GenerationStatus;
import vgandolfi.dev.flashcards.domain.model.enums.SourceType;

/**
 * Job assincrono de geracao de flashcards. Criado com status {@code PENDING} e
 * retornado ao cliente (HTTP 202) para polling via
 * {@code GET /api/v1/generations/{id}}.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "generation_jobs")
public class GenerationJob extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_file_id")
    private SourceFile sourceFile;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private GenerationStatus status = GenerationStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id")
    private Deck deck;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private SourceType sourceType;

    /** Texto informado diretamente pelo usuario (jobs text-only). */
    @Column(name = "input_text", columnDefinition = "TEXT")
    private String inputText;

    /** Titulo opcional do deck resultante. */
    @Column(name = "title", length = 255)
    private String title;
}
