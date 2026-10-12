package vgandolfi.dev.flashcards.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vgandolfi.dev.flashcards.application.dto.response.CardResponse;
import vgandolfi.dev.flashcards.domain.model.Card;
import vgandolfi.dev.flashcards.domain.repository.CardRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Selecao dos cards devidos (due) para estudo. */
@Service
public class StudyService {

    private final CardRepository cardRepository;

    public StudyService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    /**
     * Cards com {@code dueDate <= hoje}, opcionalmente filtrados por deck e/ou
     * tag (case-insensitive).
     */
    @Transactional(readOnly = true)
    public List<CardResponse> due(UUID deckId, String tag) {
        LocalDate today = LocalDate.now();
        boolean hasTag = tag != null && !tag.isBlank();
        List<Card> cards;
        if (hasTag && deckId != null) {
            cards = cardRepository.findDueByDeckIdAndTagName(deckId, today, tag);
        } else if (hasTag) {
            cards = cardRepository.findDueByTagName(today, tag);
        } else if (deckId != null) {
            cards = cardRepository.findByDeckIdAndDueDateLessThanEqualOrderByDueDateAsc(deckId, today);
        } else {
            cards = cardRepository.findByDueDateLessThanEqualOrderByDueDateAsc(today);
        }
        return cards.stream().map(CardResponse::from).toList();
    }
}
