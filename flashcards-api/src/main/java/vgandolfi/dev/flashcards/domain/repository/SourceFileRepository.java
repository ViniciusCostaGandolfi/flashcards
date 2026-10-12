package vgandolfi.dev.flashcards.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vgandolfi.dev.flashcards.domain.model.SourceFile;

import java.util.List;
import java.util.UUID;

public interface SourceFileRepository extends JpaRepository<SourceFile, UUID> {

    List<SourceFile> findAllByOrderByUploadedAtDesc();
}
