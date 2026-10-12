package vgandolfi.dev.flashcards.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vgandolfi.dev.flashcards.application.dto.response.CardResponse;
import vgandolfi.dev.flashcards.domain.model.Card;
import vgandolfi.dev.flashcards.domain.model.Deck;
import vgandolfi.dev.flashcards.domain.model.Tag;
import vgandolfi.dev.flashcards.domain.model.enums.CardType;
import vgandolfi.dev.flashcards.domain.repository.CardRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudyServiceTest {

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private StudyService studyService;

    private final UUID deckId = UUID.randomUUID();

    private Card card() {
        Card card = new Card();
        card.setId(UUID.randomUUID());
        Deck deck = new Deck();
        deck.setId(deckId);
        card.setDeck(deck);
        card.setType(CardType.CLOZE);
        card.setQuestion("q");
        card.setAnswer("a");
        card.setDueDate(LocalDate.now());
        Tag tag = new Tag();
        tag.setId(UUID.randomUUID());
        tag.setName("java");
        card.getTags().add(tag);
        return card;
    }

    @Test
    void dueWithoutDeckReturnsAllDueCards() {
        when(cardRepository.findByDueDateLessThanEqualOrderByDueDateAsc(LocalDate.now()))
                .thenReturn(List.of(card()));

        List<CardResponse> result = studyService.due(null, null);

        assertThat(result).hasSize(1);
        verify(cardRepository).findByDueDateLessThanEqualOrderByDueDateAsc(LocalDate.now());
    }

    @Test
    void dueWithDeckFiltersByDeck() {
        when(cardRepository.findByDeckIdAndDueDateLessThanEqualOrderByDueDateAsc(deckId, LocalDate.now()))
                .thenReturn(List.of(card()));

        List<CardResponse> result = studyService.due(deckId, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).deckId()).isEqualTo(deckId);
        verify(cardRepository).findByDeckIdAndDueDateLessThanEqualOrderByDueDateAsc(deckId, LocalDate.now());
    }

    @Test
    void dueWithTagFiltersByTag() {
        when(cardRepository.findDueByTagName(LocalDate.now(), "java")).thenReturn(List.of(card()));

        List<CardResponse> result = studyService.due(null, "java");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).tags()).containsExactly("java");
        verify(cardRepository).findDueByTagName(LocalDate.now(), "java");
    }

    @Test
    void dueWithDeckAndTagFiltersByBoth() {
        when(cardRepository.findDueByDeckIdAndTagName(deckId, LocalDate.now(), "java")).thenReturn(List.of(card()));

        List<CardResponse> result = studyService.due(deckId, "java");

        assertThat(result).hasSize(1);
        verify(cardRepository).findDueByDeckIdAndTagName(deckId, LocalDate.now(), "java");
    }
}
