package vgandolfi.dev.flashcards.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Configuracoes globais da aplicacao (linha unica / singleton). Mantida em
 * banco para permitir edicao via {@code PUT /api/v1/settings} sem redeploy.
 *
 * @see vgandolfi.dev.flashcards.application.service.SettingsService
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "app_settings")
public class AppSettings extends BaseEntity {

    /** Menor numero de cartoes aceito por geracao. */
    public static final int MIN_CARDS_PER_GENERATION = 3;

    public static final int DEFAULT_CARDS_PER_GENERATION = 6;
    public static final String DEFAULT_LANGUAGE = "pt-BR";
    public static final String DEFAULT_PDF_ENGINE = "native";

    /** Modelo usado para OCR/leitura de imagens e PDFs. */
    @Column(name = "reader_model", length = 120)
    private String readerModel;

    /** Modelo usado para gerar os flashcards. */
    @Column(name = "generator_model", length = 120)
    private String generatorModel;

    /** Prompt base enviado ao modelo gerador. */
    @Column(name = "base_prompt", columnDefinition = "TEXT")
    private String basePrompt;

    @Column(name = "cards_per_generation", nullable = false)
    private int cardsPerGeneration = DEFAULT_CARDS_PER_GENERATION;

    @Column(name = "language", nullable = false, length = 20)
    private String language = DEFAULT_LANGUAGE;

    @Column(name = "pdf_engine", nullable = false, length = 30)
    private String pdfEngine = DEFAULT_PDF_ENGINE;
}
