package vgandolfi.dev.flashcards.domain.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import vgandolfi.dev.flashcards.domain.model.enums.CardType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Flashcard pertencente a um {@link Deck}. As opcoes (apenas para
 * {@link CardType#MULTIPLE_CHOICE}) sao uma colecao ordenada de strings.
 *
 * <p>Os campos de SRS (spaced repetition system) sao atualizados pelo
 * {@link vgandolfi.dev.flashcards.domain.service.SrsScheduler} a cada revisao.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cards")
public class Card extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deck_id", nullable = false)
    private Deck deck;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private CardType type;

    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "answer", nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "hint", length = 500)
    private String hint;

    @Column(name = "position", nullable = false)
    private int position;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "card_options", joinColumns = @JoinColumn(name = "card_id"))
    @Column(name = "option_value", columnDefinition = "TEXT")
    @OrderColumn(name = "option_index")
    @BatchSize(size = 32)
    private List<String> options = new ArrayList<>();

    /**
     * Tags reutilizaveis (entidade {@link Tag}). Owned side da relacao
     * {@code card_tags}. Carregada em batch ({@link BatchSize}) para evitar
     * N+1 ao listar decks/cards.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "card_tags",
            joinColumns = @JoinColumn(name = "card_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @BatchSize(size = 32)
    private Set<Tag> tags = new LinkedHashSet<>();

    // ---- SRS (SM-2 simplificado) ----

    @Column(name = "ease_factor", nullable = false)
    private double easeFactor = 2.5;

    @Column(name = "interval_days", nullable = false)
    private int intervalDays = 0;

    @Column(name = "repetitions", nullable = false)
    private int repetitions = 0;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate = LocalDate.now();

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;
}
