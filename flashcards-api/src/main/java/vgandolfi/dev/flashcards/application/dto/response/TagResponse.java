package vgandolfi.dev.flashcards.application.dto.response;

import vgandolfi.dev.flashcards.domain.model.Tag;

import java.util.UUID;

/** Tag com a contagem de cards associados. */
public record TagResponse(UUID id, String name, long cardsCount) {

    public static TagResponse of(Tag tag, long cardsCount) {
        return new TagResponse(tag.getId(), tag.getName(), cardsCount);
    }
}
