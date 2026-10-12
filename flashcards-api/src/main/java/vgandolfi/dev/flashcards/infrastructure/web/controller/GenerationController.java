package vgandolfi.dev.flashcards.infrastructure.web.controller;

import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vgandolfi.dev.flashcards.application.dto.response.CreateGenerationResponse;
import vgandolfi.dev.flashcards.application.dto.response.GenerationJobResponse;
import vgandolfi.dev.flashcards.application.service.GenerationService;
import vgandolfi.dev.flashcards.domain.exception.BusinessException;
import vgandolfi.dev.flashcards.domain.exception.EntityNotFoundException;
import vgandolfi.dev.flashcards.domain.model.GenerationJob;
import vgandolfi.dev.flashcards.domain.repository.CardRepository;
import vgandolfi.dev.flashcards.domain.repository.GenerationJobRepository;
import vgandolfi.dev.flashcards.infrastructure.security.AuthenticatedUser;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** Criacao (202 + polling) e consulta de jobs de geracao. */
@RestController
@RequestMapping("/api/v1/generations")
public class GenerationController {

    private final GenerationService generationService;
    private final GenerationJobRepository generationJobRepository;
    private final CardRepository cardRepository;

    public GenerationController(GenerationService generationService,
                                GenerationJobRepository generationJobRepository,
                                CardRepository cardRepository) {
        this.generationService = generationService;
        this.generationJobRepository = generationJobRepository;
        this.cardRepository = cardRepository;
    }

    /**
     * Cria um job. Aceita {@code multipart/form-data} com EITHER {@code file}
     * (imagem/PDF) OR {@code text} (e {@code title} opcional). {@code tags} e
     * uma lista opcional de nomes separados por virgula, aplicada a todos os
     * cards gerados.
     */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<CreateGenerationResponse> create(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "tags", required = false) String tags) throws IOException {
        if (principal == null) {
            throw new BusinessException("Usuario nao autenticado");
        }
        byte[] bytes = (file != null && !file.isEmpty()) ? file.getBytes() : null;
        String filename = (file != null) ? file.getOriginalFilename() : null;
        String contentType = (file != null) ? file.getContentType() : null;

        UUID jobId = generationService.create(principal.userId(), filename, contentType, bytes, text, title,
                parseTags(tags));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new CreateGenerationResponse(jobId));
    }

    /** Converte "a, b, a" em ["a", "b"] (trim, ignora vazios). */
    private List<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<GenerationJobResponse> get(@PathVariable @NotNull UUID id) {
        GenerationJob job = generationJobRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Job nao encontrado: " + id));
        long cardsCount = job.getDeck() == null ? 0 : cardRepository.countByDeckId(job.getDeck().getId());
        return ResponseEntity.ok(GenerationJobResponse.from(job, cardsCount));
    }
}
