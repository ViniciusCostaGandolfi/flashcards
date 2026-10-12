package vgandolfi.dev.flashcards.infrastructure.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vgandolfi.dev.flashcards.application.dto.response.CardResponse;
import vgandolfi.dev.flashcards.application.dto.response.DeckResponse;
import vgandolfi.dev.flashcards.application.dto.response.DeckSummaryResponse;
import vgandolfi.dev.flashcards.application.service.DeckService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/decks")
public class DeckController {

    private final DeckService deckService;

    public DeckController(DeckService deckService) {
        this.deckService = deckService;
    }

    @GetMapping
    public ResponseEntity<List<DeckSummaryResponse>> list(
            @RequestParam(value = "tag", required = false) String tag) {
        return ResponseEntity.ok(deckService.list(tag));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeckResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(deckService.get(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deckService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/cards")
    public ResponseEntity<List<CardResponse>> cards(@PathVariable UUID id,
                                                    @RequestParam(value = "tag", required = false) String tag) {
        return ResponseEntity.ok(deckService.cards(id, tag));
    }
}
