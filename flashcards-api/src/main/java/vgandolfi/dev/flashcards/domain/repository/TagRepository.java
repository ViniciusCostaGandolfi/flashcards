package vgandolfi.dev.flashcards.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vgandolfi.dev.flashcards.domain.model.Tag;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID> {

    /** Busca case-insensitive pelo nome de exibicao. */
    Optional<Tag> findByNameIgnoreCase(String name);

    /** Busca pela forma normalizada (minuscula/trim). */
    Optional<Tag> findByNameNormalized(String nameNormalized);

    List<Tag> findAllByOrderByNameAsc();

    /**
     * Contagem de cards por tag (projecao). Tags sem cards nao aparecem no
     * resultado — o {@code TagService} assume 0 para elas.
     */
    @Query("select t.id as id, count(c.id) as cardsCount from Card c join c.tags t group by t.id")
    List<TagCardCount> countCardsByTag();

    /** Projecao {@code {id, cardsCount}}. */
    interface TagCardCount {
        UUID getId();

        long getCardsCount();
    }
}
