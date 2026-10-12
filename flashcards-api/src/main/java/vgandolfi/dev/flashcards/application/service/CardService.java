package vgandolfi.dev.flashcards.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vgandolfi.dev.flashcards.application.dto.request.CreateCardRequest;
import vgandolfi.dev.flashcards.application.dto.request.UpdateCardRequest;
import vgandolfi.dev.flashcards.application.dto.response.CardResponse;
import vgandolfi.dev.flashcards.domain.exception.EntityNotFoundException;
import vgandolfi.dev.flashcards.domain.model.Card;
import vgandolfi.dev.flashcards.domain.model.Deck;
import vgandolfi.dev.flashcards.domain.model.Tag;
import vgandolfi.dev.flashcards.domain.repository.CardRepository;
import vgandolfi.dev.flashcards.domain.repository.DeckRepository;
import vgandolfi.dev.flashcards.domain.service.ReviewGrade;
import vgandolfi.dev.flashcards.domain.service.SrsScheduler;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** CRUD de cards, tags e aplicacao do SRS na revisao. */
@Service
public class CardService {

    private final CardRepository cardRepository;
    private final DeckRepository deckRepository;
    private final SrsScheduler srsScheduler;
    private final TagService tagService;

    public CardService(CardRepository cardRepository, DeckRepository deckRepository, SrsScheduler srsScheduler,
                       TagService tagService) {
        this.cardRepository = cardRepository;
        this.deckRepository = deckRepository;
        this.srsScheduler = srsScheduler;
        this.tagService = tagService;
    }

    @Transactional
    public CardResponse create(CreateCardRequest request) {
        Deck deck = deckRepository.findById(request.deckId())
                .orElseThrow(() -> new EntityNotFoundException("Deck nao encontrado: " + request.deckId()));
        Card card = new Card();
        card.setDeck(deck);
        card.setType(request.type());
        card.setQuestion(request.question());
        card.setAnswer(request.answer());
        card.setExplanation(request.explanation());
        card.setHint(request.hint());
        card.setPosition(request.position() != null ? request.position()
                : (int) cardRepository.countByDeckId(deck.getId()));
        applyOptions(card, request.options());
        applyTags(card, tagService.resolveTags(request.tags()));
        return CardResponse.from(cardRepository.save(card));
    }

    @Transactional
    public CardResponse update(UUID id, UpdateCardRequest request) {
        Card card = require(id);
        card.setType(request.type());
        card.setQuestion(request.question());
        card.setAnswer(request.answer());
        card.setExplanation(request.explanation());
        card.setHint(request.hint());
        if (request.position() != null) {
            card.setPosition(request.position());
        }
        applyOptions(card, request.options());
        applyTags(card, tagService.resolveTags(request.tags()));
        return CardResponse.from(cardRepository.save(card));
    }

    @Transactional
    public void delete(UUID id) {
        Card card = require(id);
        cardRepository.delete(card);
    }

    /** Aplica a nota de revisao (SRS) e recalcula a proxima data. */
    @Transactional
    public CardResponse review(UUID id, ReviewGrade grade) {
        Card card = require(id);
        srsScheduler.apply(card, grade, Instant.now());
        return CardResponse.from(cardRepository.save(card));
    }

    private void applyOptions(Card card, List<String> options) {
        card.getOptions().clear();
        if (options != null) {
            options.stream()
                    .filter(option -> option != null && !option.isBlank())
                    .forEach(card.getOptions()::add);
        }
    }

    private void applyTags(Card card, Set<Tag> tags) {
        card.getTags().clear();
        card.getTags().addAll(tags);
    }

    private Card require(UUID id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Card nao encontrado: " + id));
    }
}
