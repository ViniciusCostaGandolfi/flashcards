package vgandolfi.dev.flashcards.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vgandolfi.dev.flashcards.domain.model.AppSettings;

import java.util.Optional;
import java.util.UUID;

public interface AppSettingsRepository extends JpaRepository<AppSettings, UUID> {

    /** A tabela mantem uma unica linha (singleton). */
    Optional<AppSettings> findFirstByOrderByCreatedAtAsc();
}
