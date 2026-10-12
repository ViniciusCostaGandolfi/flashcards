package vgandolfi.dev.flashcards.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vgandolfi.dev.flashcards.domain.model.GenerationJob;

import java.util.UUID;

public interface GenerationJobRepository extends JpaRepository<GenerationJob, UUID> {
}
