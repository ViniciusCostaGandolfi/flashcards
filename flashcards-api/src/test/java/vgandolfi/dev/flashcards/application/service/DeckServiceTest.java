package vgandolfi.dev.flashcards.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vgandolfi.dev.flashcards.application.dto.response.CardResponse;
import vgandolfi.dev.flashcards.application.dto.response.DeckResponse;
import vgandolfi.dev.flashcards.application.dto.response.DeckSummaryResponse;
import vgandolfi.dev.flashcards.domain.exception.EntityNotFoundException;
import vgandolfi.dev.flashcards.domain.model.Card;
import vgandolfi.dev.flashcards.domain.model.Deck;
import vgandolfi.dev.flashcards.domain.model.Tag;
import vgandolfi.dev.flashcards.domain.model.enums.CardType;
import vgandolfi.dev.flashcards.domain.model.enums.SourceType;
import vgandolfi.dev.flashcards.domain.repository.CardRepository;
import vgandolfi.dev.flashcards.domain.repository.DeckRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeckServiceTest {

    @Mock
    private DeckRepository deckRepository;
    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private DeckService deckService;

    private final UUID deckId = UUID.randomUUID();

    private Deck deck() {
        Deck deck = new Deck();
        deck.setId(deckId);
        deck.setTitle("Deck");
        deck.setSourceType(SourceType.TEXT);
        deck.setCreatedAt(Instant.now());
        return deck;
    }

    private Card card() {
        Card card = new Card();
        card.setId(UUID.randomUUID());
        card.setDeck(deck());
        card.setType(CardType.CLOZE);
        card.setQuestion("q");
        card.setAnswer("a");
        card.setPosition(0);
        Tag tag = new Tag();
        tag.setId(UUID.randomUUID());
        tag.setName("java");
        card.getTags().add(tag);
        return card;
    }

    @Test
    void listReturnsSummariesWithCounts() {
        when(deckRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(deck()));
        when(cardRepository.countByDeckId(deckId)).thenReturn(3L);
        when(cardRepository.countByDeckIdAndDueDateLessThanEqual(deckId, LocalDate.now())).thenReturn(2L);

        List<DeckSummaryResponse> result = deckService.list(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).cardsCount()).isEqualTo(3L);
        assertThat(result.get(0).dueCount()).isEqualTo(2L);
    }

    @Test
    void listFiltersByTag() {
        when(cardRepository.findDecksByTagName("java")).thenReturn(List.of(deck()));
        when(cardRepository.countByDeckId(deckId)).thenReturn(1L);
        when(cardRepository.countByDeckIdAndDueDateLessThanEqual(deckId, LocalDate.now())).thenReturn(1L);

        List<DeckSummaryResponse> result = deckService.list("java");

        assertThat(result).hasSize(1);
        verify(cardRepository).findDecksByTagName("java");
    }

    @Test
    void getReturnsDeckDetailWithTags() {
        when(deckRepository.findById(deckId)).thenReturn(Optional.of(deck()));
        when(cardRepository.countByDeckId(deckId)).thenReturn(3L);
        when(cardRepository.countByDeckIdAndDueDateLessThanEqual(deckId, LocalDate.now())).thenReturn(1L);
        when(cardRepository.findDistinctTagNamesByDeckId(deckId)).thenReturn(List.of("java", "spring"));

        DeckResponse response = deckService.get(deckId);

        assertThat(response.title()).isEqualTo("Deck");
        assertThat(response.cardsCount()).isEqualTo(3L);
        assertThat(response.tags()).containsExactly("java", "spring");
    }

    @Test
    void getUnknownDeckThrows() {
        when(deckRepository.findById(deckId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deckService.get(deckId)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void cardsReturnsOrderedCards() {
        when(deckRepository.findById(deckId)).thenReturn(Optional.of(deck()));
        when(cardRepository.findByDeckIdOrderByPositionAsc(deckId)).thenReturn(List.of(card()));

        List<CardResponse> result = deckService.cards(deckId, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).tags()).containsExactly("java");
    }

    @Test
    void cardsFiltersByTag() {
        when(deckRepository.findById(deckId)).thenReturn(Optional.of(deck()));
        when(cardRepository.findByDeckIdAndTagName(deckId, "java")).thenReturn(List.of(card()));

        List<CardResponse> result = deckService.cards(deckId, "java");

        assertThat(result).hasSize(1);
        verify(cardRepository).findByDeckIdAndTagName(deckId, "java");
    }

    @Test
    void deleteRemovesCardsAndDeck() {
        Deck deck = deck();
        when(deckRepository.findById(deckId)).thenReturn(Optional.of(deck));

        deckService.delete(deckId);

        verify(cardRepository).deleteByDeckId(deckId);
        verify(deckRepository).delete(deck);
    }
}
