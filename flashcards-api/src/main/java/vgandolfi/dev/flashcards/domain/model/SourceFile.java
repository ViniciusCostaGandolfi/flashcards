package vgandolfi.dev.flashcards.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Metadados de um arquivo enviado (imagem ou PDF) e armazenado em object
 * storage (S3/MinIO). O binario nunca fica no banco: apenas a {@code storageKey}.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "source_files")
public class SourceFile extends BaseEntity {

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "content_type", length = 120)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;
}
