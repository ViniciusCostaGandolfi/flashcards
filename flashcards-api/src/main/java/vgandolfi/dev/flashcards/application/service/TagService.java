package vgandolfi.dev.flashcards.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vgandolfi.dev.flashcards.application.dto.request.CreateTagRequest;
import vgandolfi.dev.flashcards.application.dto.response.TagResponse;
import vgandolfi.dev.flashcards.domain.exception.ConflictException;
import vgandolfi.dev.flashcards.domain.exception.EntityNotFoundException;
import vgandolfi.dev.flashcards.domain.model.Tag;
import vgandolfi.dev.flashcards.domain.repository.TagRepository;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Gestao de tags e resolucao "create-or-reuse" (case-insensitive).
 *
 * <p>E o unico ponto de criacao de {@link Tag}, garantindo que
 * {@code name_normalized} fique sempre consistente com {@code name}.</p>
 */
@Service
public class TagService {

    private static final Logger log = LoggerFactory.getLogger(TagService.class);

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Transactional(readOnly = true)
    public List<TagResponse> list() {
        Map<UUID, Long> counts = tagRepository.countCardsByTag().stream()
                .collect(Collectors.toMap(TagRepository.TagCardCount::getId,
                        TagRepository.TagCardCount::getCardsCount));
        return tagRepository.findAllByOrderByNameAsc().stream()
                .map(tag -> TagResponse.of(tag, counts.getOrDefault(tag.getId(), 0L)))
                .toList();
    }

    @Transactional
    public TagResponse create(CreateTagRequest request) {
        String name = request.name().trim();
        if (tagRepository.findByNameIgnoreCase(name).isPresent()) {
            throw new ConflictException("Tag ja existe: " + name);
        }
        Tag saved = tagRepository.save(newTag(name));
        log.info("tag_created tagId={} name={}", saved.getId(), saved.getName());
        return TagResponse.of(saved, 0L);
    }

    @Transactional
    public void delete(UUID id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tag nao encontrada: " + id));
        // ON DELETE CASCADE em card_tags remove as associacoes (detach).
        tagRepository.delete(tag);
        log.info("tag_deleted tagId={}", id);
    }

    /**
     * Resolve (reutilizando ou criando) as tags informadas por nome,
     * case-insensitive e sem duplicatas. Nomes nulos/em branco sao ignorados.
     */
    @Transactional
    public Set<Tag> resolveTags(Collection<String> names) {
        Set<Tag> result = new LinkedHashSet<>();
        if (names == null || names.isEmpty()) {
            return result;
        }
        // Deduplica por forma normalizada, preservando a primeira grafia.
        Map<String, String> byNormalized = new LinkedHashMap<>();
        for (String raw : names) {
            if (raw == null) {
                continue;
            }
            String name = raw.trim();
            if (name.isEmpty()) {
                continue;
            }
            byNormalized.putIfAbsent(normalize(name), name);
        }
        for (Map.Entry<String, String> entry : byNormalized.entrySet()) {
            String normalized = entry.getKey();
            Tag tag = tagRepository.findByNameNormalized(normalized)
                    .orElseGet(() -> tagRepository.save(newTag(entry.getValue())));
            result.add(tag);
        }
        return result;
    }

    private Tag newTag(String name) {
        Tag tag = new Tag();
        tag.setName(name);
        tag.setNameNormalized(normalize(name));
        return tag;
    }

    private String normalize(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
