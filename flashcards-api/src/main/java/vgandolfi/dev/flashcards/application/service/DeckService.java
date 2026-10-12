package vgandolfi.dev.flashcards.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vgandolfi.dev.flashcards.application.dto.response.CardResponse;
import vgandolfi.dev.flashcards.application.dto.response.DeckResponse;
import vgandolfi.dev.flashcards.application.dto.response.DeckSummaryResponse;
import vgandolfi.dev.flashcards.domain.exception.EntityNotFoundException;
import vgandolfi.dev.flashcards.domain.model.Card;
import vgandolfi.dev.flashcards.domain.model.Deck;
import vgandolfi.dev.flashcards.domain.repository.CardRepository;
import vgandolfi.dev.flashcards.domain.repository.DeckRepository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Consulta e remocao de decks. */
@Service
public class DeckService {

    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;

    public DeckService(DeckRepository deckRepository, CardRepository cardRepository) {
        this.deckRepository = deckRepository;
        this.cardRepository = cardRepository;
    }

    /**
     * Lista decks (mais recentes primeiro). Se {@code tag} for informada,
     * retorna apenas decks com ao menos um card dessa tag (case-insensitive).
     */
    @Transactional(readOnly = true)
    public List<DeckSummaryResponse> list(String tag) {
        LocalDate today = LocalDate.now();
        List<Deck> decks = (tag == null || tag.isBlank())
                ? deckRepository.findAllByOrderByCreatedAtDesc()
                : cardRepository.findDecksByTagName(tag).stream()
                        .sorted(Comparator.comparing(Deck::getCreatedAt).reversed())
                        .toList();
        return decks.stream()
                .map(deck -> DeckSummaryResponse.from(deck,
                        cardRepository.countByDeckId(deck.getId()),
                        cardRepository.countByDeckIdAndDueDateLessThanEqual(deck.getId(), today)))
                .toList();
    }

    @Transactional(readOnly = true)
    public DeckResponse get(UUID id) {
        Deck deck = require(id);
        LocalDate today = LocalDate.now();
        return DeckResponse.from(deck,
                cardRepository.countByDeckId(deck.getId()),
                cardRepository.countByDeckIdAndDueDateLessThanEqual(deck.getId(), today),
                cardRepository.findDistinctTagNamesByDeckId(deck.getId()));
    }

    /**
     * Cards de um deck (por posicao), opcionalmente filtrados por tag
     * (case-insensitive).
     */
    @Transactional(readOnly = true)
    public List<CardResponse> cards(UUID deckId, String tag) {
        require(deckId);
        List<Card> cards = (tag == null || tag.isBlank())
                ? cardRepository.findByDeckIdOrderByPositionAsc(deckId)
                : cardRepository.findByDeckIdAndTagName(deckId, tag);
        return cards.stream().map(CardResponse::from).toList();
    }

    @Transactional
    public void delete(UUID id) {
        Deck deck = require(id);
        cardRepository.deleteByDeckId(deck.getId());
        deckRepository.delete(deck);
    }

    private Deck require(UUID id) {
        return deckRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Deck nao encontrado: " + id));
    }
}
