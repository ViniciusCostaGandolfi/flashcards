package vgandolfi.dev.flashcards.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vgandolfi.dev.flashcards.domain.model.Card;
import vgandolfi.dev.flashcards.domain.model.Deck;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, UUID> {

    List<Card> findByDeckIdOrderByPositionAsc(UUID deckId);

    long countByDeckId(UUID deckId);

    long countByDeckIdAndDueDateLessThanEqual(UUID deckId, LocalDate date);

    List<Card> findByDeckIdAndDueDateLessThanEqualOrderByDueDateAsc(UUID deckId, LocalDate date);

    List<Card> findByDueDateLessThanEqualOrderByDueDateAsc(LocalDate date);

    void deleteByDeckId(UUID deckId);

    // ---- Filtros por tag (case-insensitive) ----

    @Query("select distinct c from Card c join c.tags t "
            + "where c.deck.id = :deckId and lower(t.name) = lower(:tag) order by c.position asc")
    List<Card> findByDeckIdAndTagName(@Param("deckId") UUID deckId, @Param("tag") String tag);

    @Query("select distinct c from Card c join c.tags t "
            + "where c.dueDate <= :date and lower(t.name) = lower(:tag) order by c.dueDate asc")
    List<Card> findDueByTagName(@Param("date") LocalDate date, @Param("tag") String tag);

    @Query("select distinct c from Card c join c.tags t "
            + "where c.deck.id = :deckId and c.dueDate <= :date and lower(t.name) = lower(:tag) "
            + "order by c.dueDate asc")
    List<Card> findDueByDeckIdAndTagName(@Param("deckId") UUID deckId, @Param("date") LocalDate date,
                                         @Param("tag") String tag);

    /** Nomes distintos de tags presentes nos cards de um deck (ordenados). */
    @Query("select distinct t.name from Card c join c.tags t where c.deck.id = :deckId order by t.name")
    List<String> findDistinctTagNamesByDeckId(@Param("deckId") UUID deckId);

    /** Decks que possuem ao menos um card com a tag informada. */
    @Query("select distinct c.deck from Card c join c.tags t where lower(t.name) = lower(:tag)")
    List<Deck> findDecksByTagName(@Param("tag") String tag);
}
