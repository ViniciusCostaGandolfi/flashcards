package vgandolfi.dev.flashcards.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vgandolfi.dev.flashcards.application.dto.response.SourceFileResponse;
import vgandolfi.dev.flashcards.domain.exception.EntityNotFoundException;
import vgandolfi.dev.flashcards.domain.model.SourceFile;
import vgandolfi.dev.flashcards.domain.repository.SourceFileRepository;
import vgandolfi.dev.flashcards.infrastructure.s3.S3Storage;

import java.util.List;
import java.util.UUID;

/** Consulta de arquivos de origem e seus bytes (via object storage). */
@Service
public class FileService {

    private final SourceFileRepository sourceFileRepository;
    private final S3Storage s3Storage;

    public FileService(SourceFileRepository sourceFileRepository, S3Storage s3Storage) {
        this.sourceFileRepository = sourceFileRepository;
        this.s3Storage = s3Storage;
    }

    @Transactional(readOnly = true)
    public List<SourceFileResponse> list() {
        return sourceFileRepository.findAllByOrderByUploadedAtDesc().stream()
                .map(SourceFileResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SourceFileResponse get(UUID id) {
        return SourceFileResponse.from(require(id));
    }

    /** Baixa o conteudo binario do arquivo (inline). */
    @Transactional(readOnly = true)
    public FileContent content(UUID id) {
        SourceFile file = require(id);
        return new FileContent(file.getOriginalFilename(), file.getContentType(),
                s3Storage.downloadBytes(file.getStorageKey()));
    }

    private SourceFile require(UUID id) {
        return sourceFileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Arquivo nao encontrado: " + id));
    }

    /** Dados binarios de um arquivo para download inline. */
    public record FileContent(String filename, String contentType, byte[] bytes) {
    }
}
