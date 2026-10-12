package vgandolfi.dev.flashcards.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vgandolfi.dev.flashcards.domain.model.enums.SourceType;

import java.util.ArrayList;
import java.util.List;

/**
 * Baralho de flashcards gerado (ou criado manualmente) a partir de um material
 * de origem. Guarda os modelos/prompt usados e, quando aplicavel, o texto
 * extraido do arquivo.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "decks")
public class Deck extends BaseEntity {

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private SourceType sourceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_file_id")
    private SourceFile sourceFile;

    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    @Column(name = "reader_model", length = 120)
    private String readerModel;

    @Column(name = "generator_model", length = 120)
    private String generatorModel;

    @Column(name = "prompt_used", columnDefinition = "TEXT")
    private String promptUsed;

    @OneToMany(mappedBy = "deck", fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    private List<Card> cards = new ArrayList<>();
}
