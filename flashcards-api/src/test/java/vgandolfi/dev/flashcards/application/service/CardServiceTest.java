package vgandolfi.dev.flashcards.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import vgandolfi.dev.flashcards.application.dto.request.CreateCardRequest;
import vgandolfi.dev.flashcards.application.dto.request.UpdateCardRequest;
import vgandolfi.dev.flashcards.application.dto.response.CardResponse;
import vgandolfi.dev.flashcards.domain.exception.EntityNotFoundException;
import vgandolfi.dev.flashcards.domain.model.Card;
import vgandolfi.dev.flashcards.domain.model.Deck;
import vgandolfi.dev.flashcards.domain.model.Tag;
import vgandolfi.dev.flashcards.domain.model.enums.CardType;
import vgandolfi.dev.flashcards.domain.repository.CardRepository;
import vgandolfi.dev.flashcards.domain.repository.DeckRepository;
import vgandolfi.dev.flashcards.domain.service.ReviewGrade;
import vgandolfi.dev.flashcards.domain.service.SrsScheduler;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;
    @Mock
    private DeckRepository deckRepository;
    @Mock
    private TagService tagService;
    @Spy
    private SrsScheduler srsScheduler = new SrsScheduler();

    @InjectMocks
    private CardService cardService;

    private final UUID deckId = UUID.randomUUID();
    private final UUID cardId = UUID.randomUUID();

    private Deck deck() {
        Deck deck = new Deck();
        deck.setId(deckId);
        return deck;
    }

    private Card card() {
        Card card = new Card();
        card.setId(cardId);
        card.setDeck(deck());
        card.setType(CardType.MULTIPLE_CHOICE);
        card.setQuestion("Pergunta?");
        card.setAnswer("Resposta");
        card.setPosition(0);
        return card;
    }

    private Tag tag(String name) {
        Tag tag = new Tag();
        tag.setId(UUID.randomUUID());
        tag.setName(name);
        return tag;
    }

    @Test
    void createPersistsCardWithOptions() {
        when(deckRepository.findById(deckId)).thenReturn(Optional.of(deck()));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        CardResponse response = cardService.create(new CreateCardRequest(deckId, CardType.MULTIPLE_CHOICE,
                "Pergunta?", "B", "expl", "hint", 0, List.of("A", "B", "C", "D"), null));

        assertThat(response.deckId()).isEqualTo(deckId);
        assertThat(response.options()).containsExactly("A", "B", "C", "D");
        assertThat(response.type()).isEqualTo(CardType.MULTIPLE_CHOICE);
    }

    @Test
    void createAttachesResolvedTags() {
        when(deckRepository.findById(deckId)).thenReturn(Optional.of(deck()));
        when(tagService.resolveTags(List.of("Java", "java"))).thenReturn(Set.of(tag("Java")));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        CardResponse response = cardService.create(new CreateCardRequest(deckId, CardType.CLOZE,
                "q", "a", null, null, 0, List.of(), List.of("Java", "java")));

        assertThat(response.tags()).containsExactly("Java");
        verify(tagService).resolveTags(List.of("Java", "java"));
    }

    @Test
    void createWithUnknownDeckThrows() {
        when(deckRepository.findById(deckId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.create(new CreateCardRequest(deckId, CardType.CLOZE,
                "q", "a", null, null, 0, List.of(), null)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateReplacesFields() {
        Card card = card();
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        CardResponse response = cardService.update(cardId, new UpdateCardRequest(CardType.TRUE_FALSE,
                "Nova?", "Verdadeiro", null, null, 2, List.of(), null));

        assertThat(response.type()).isEqualTo(CardType.TRUE_FALSE);
        assertThat(response.question()).isEqualTo("Nova?");
        assertThat(response.position()).isEqualTo(2);
    }

    @Test
    void updateReplacesTagSet() {
        Card card = card();
        card.getTags().add(tag("antiga"));
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(tagService.resolveTags(List.of("nova"))).thenReturn(Set.of(tag("nova")));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        CardResponse response = cardService.update(cardId, new UpdateCardRequest(CardType.TRUE_FALSE,
                "q", "a", null, null, 0, List.of(), List.of("nova")));

        assertThat(response.tags()).containsExactly("nova");
        assertThat(card.getTags()).hasSize(1);
    }

    @Test
    void deleteRemovesCard() {
        Card card = card();
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        cardService.delete(cardId);

        verify(cardRepository).delete(card);
    }

    @Test
    void reviewAppliesSrsSchedule() {
        Card card = card();
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        CardResponse response = cardService.review(cardId, ReviewGrade.EASY);

        assertThat(response.repetitions()).isEqualTo(1);
        assertThat(response.intervalDays()).isEqualTo(1);
        assertThat(response.lastReviewedAt()).isNotNull();
    }

    @Test
    void reviewUnknownCardThrows() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.review(cardId, ReviewGrade.AGAIN))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
