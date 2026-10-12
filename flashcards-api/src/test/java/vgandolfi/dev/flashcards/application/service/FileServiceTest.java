package vgandolfi.dev.flashcards.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vgandolfi.dev.flashcards.application.dto.response.SourceFileResponse;
import vgandolfi.dev.flashcards.domain.exception.EntityNotFoundException;
import vgandolfi.dev.flashcards.domain.model.SourceFile;
import vgandolfi.dev.flashcards.domain.repository.SourceFileRepository;
import vgandolfi.dev.flashcards.infrastructure.s3.S3Storage;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private SourceFileRepository sourceFileRepository;
    @Mock
    private S3Storage s3Storage;

    @InjectMocks
    private FileService fileService;

    private final UUID fileId = UUID.randomUUID();

    private SourceFile sourceFile() {
        SourceFile file = new SourceFile();
        file.setId(fileId);
        file.setOriginalFilename("material.pdf");
        file.setContentType("application/pdf");
        file.setSizeBytes(3L);
        file.setStorageKey("uploads/x/material.pdf");
        file.setUploadedAt(Instant.now());
        return file;
    }

    @Test
    void listReturnsFiles() {
        when(sourceFileRepository.findAllByOrderByUploadedAtDesc()).thenReturn(List.of(sourceFile()));

        List<SourceFileResponse> result = fileService.list();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).originalFilename()).isEqualTo("material.pdf");
    }

    @Test
    void getReturnsFileMetadata() {
        when(sourceFileRepository.findById(fileId)).thenReturn(Optional.of(sourceFile()));

        SourceFileResponse response = fileService.get(fileId);

        assertThat(response.contentType()).isEqualTo("application/pdf");
    }

    @Test
    void contentDownloadsBytesFromStorage() {
        when(sourceFileRepository.findById(fileId)).thenReturn(Optional.of(sourceFile()));
        when(s3Storage.downloadBytes("uploads/x/material.pdf")).thenReturn(new byte[] {1, 2, 3});

        FileService.FileContent content = fileService.content(fileId);

        assertThat(content.bytes()).containsExactly(1, 2, 3);
        assertThat(content.contentType()).isEqualTo("application/pdf");
    }

    @Test
    void getUnknownFileThrows() {
        when(sourceFileRepository.findById(fileId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fileService.get(fileId)).isInstanceOf(EntityNotFoundException.class);
    }
}
