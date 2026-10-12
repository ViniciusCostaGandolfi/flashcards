package vgandolfi.dev.flashcards.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vgandolfi.dev.flashcards.domain.model.Deck;

import java.util.List;
import java.util.UUID;

public interface DeckRepository extends JpaRepository<Deck, UUID> {

    List<Deck> findAllByOrderByCreatedAtDesc();
}
