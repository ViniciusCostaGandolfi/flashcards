package vgandolfi.dev.flashcards.infrastructure.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vgandolfi.dev.flashcards.application.dto.response.CardResponse;
import vgandolfi.dev.flashcards.application.service.StudyService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/study")
public class StudyController {

    private final StudyService studyService;

    public StudyController(StudyService studyService) {
        this.studyService = studyService;
    }

    @GetMapping("/due")
    public ResponseEntity<List<CardResponse>> due(
            @RequestParam(value = "deckId", required = false) UUID deckId,
            @RequestParam(value = "tag", required = false) String tag) {
        return ResponseEntity.ok(studyService.due(deckId, tag));
    }
}
