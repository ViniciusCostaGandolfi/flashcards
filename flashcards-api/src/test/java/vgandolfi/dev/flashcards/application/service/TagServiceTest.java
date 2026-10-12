package vgandolfi.dev.flashcards.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vgandolfi.dev.flashcards.application.dto.request.CreateTagRequest;
import vgandolfi.dev.flashcards.application.dto.response.TagResponse;
import vgandolfi.dev.flashcards.domain.exception.ConflictException;
import vgandolfi.dev.flashcards.domain.exception.EntityNotFoundException;
import vgandolfi.dev.flashcards.domain.model.Tag;
import vgandolfi.dev.flashcards.domain.repository.TagRepository;
import vgandolfi.dev.flashcards.domain.repository.TagRepository.TagCardCount;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private TagService tagService;

    private final UUID tagId = UUID.randomUUID();

    private Tag tag(String name) {
        Tag tag = new Tag();
        tag.setId(UUID.randomUUID());
        tag.setName(name);
        tag.setNameNormalized(name.toLowerCase());
        return tag;
    }

    private TagCardCount count(UUID id, long cardsCount) {
        TagCardCount projection = mock(TagCardCount.class);
        when(projection.getId()).thenReturn(id);
        when(projection.getCardsCount()).thenReturn(cardsCount);
        return projection;
    }

    @Test
    void listReturnsTagsWithCounts() {
        Tag java = tag("Java");
        Tag spring = tag("Spring");
        TagCardCount projection = count(java.getId(), 3L);
        when(tagRepository.findAllByOrderByNameAsc()).thenReturn(List.of(java, spring));
        when(tagRepository.countCardsByTag()).thenReturn(List.of(projection));

        List<TagResponse> result = tagService.list();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("Java");
        assertThat(result.get(0).cardsCount()).isEqualTo(3L);
        assertThat(result.get(1).cardsCount()).isZero();
    }

    @Test
    void createPersistsNormalizedTag() {
        when(tagRepository.findByNameIgnoreCase("Java")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> {
            Tag saved = inv.getArgument(0);
            saved.setId(tagId);
            return saved;
        });

        TagResponse response = tagService.create(new CreateTagRequest("  Java  "));

        assertThat(response.name()).isEqualTo("Java");
        assertThat(response.cardsCount()).isZero();
    }

    @Test
    void createDuplicateIsCaseInsensitive() {
        when(tagRepository.findByNameIgnoreCase("java")).thenReturn(Optional.of(tag("Java")));

        assertThatThrownBy(() -> tagService.create(new CreateTagRequest("java")))
                .isInstanceOf(ConflictException.class);
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    void deleteRemovesTag() {
        Tag existing = tag("Java");
        when(tagRepository.findById(tagId)).thenReturn(Optional.of(existing));

        tagService.delete(tagId);

        verify(tagRepository).delete(existing);
    }

    @Test
    void deleteUnknownTagThrows() {
        when(tagRepository.findById(tagId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagService.delete(tagId)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void resolveTagsCreatesMissingTagsAndDeduplicates() {
        when(tagRepository.findByNameNormalized("java")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> {
            Tag saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        Set<Tag> resolved = tagService.resolveTags(List.of("Java", " java ", "JAVA"));

        assertThat(resolved).hasSize(1);
        assertThat(resolved.iterator().next().getName()).isEqualTo("Java");
        verify(tagRepository).save(any(Tag.class));
    }

    @Test
    void resolveTagsReusesExistingTag() {
        Tag existing = tag("Spring");
        when(tagRepository.findByNameNormalized("spring")).thenReturn(Optional.of(existing));

        Set<Tag> resolved = tagService.resolveTags(List.of("SPRING"));

        assertThat(resolved).containsExactly(existing);
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    void resolveTagsIgnoresBlankAndNull() {
        Set<Tag> resolved = tagService.resolveTags(java.util.Arrays.asList(null, "  ", ""));

        assertThat(resolved).isEmpty();
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    void resolveTagsWithNullReturnsEmpty() {
        assertThat(tagService.resolveTags(null)).isEmpty();
    }
}
