package vgandolfi.dev.flashcards.application.dto.response;

import vgandolfi.dev.flashcards.domain.model.SourceFile;

import java.time.Instant;
import java.util.UUID;

public record SourceFileResponse(
        UUID id,
        String originalFilename,
        String contentType,
        long sizeBytes,
        String storageKey,
        Instant uploadedAt) {

    public static SourceFileResponse from(SourceFile file) {
        return new SourceFileResponse(
                file.getId(),
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSizeBytes(),
                file.getStorageKey(),
                file.getUploadedAt());
    }
}
