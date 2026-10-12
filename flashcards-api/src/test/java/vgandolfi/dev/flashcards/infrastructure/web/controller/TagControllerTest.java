package vgandolfi.dev.flashcards.infrastructure.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vgandolfi.dev.flashcards.application.dto.response.TagResponse;
import vgandolfi.dev.flashcards.application.service.TagService;
import vgandolfi.dev.flashcards.domain.exception.ConflictException;
import vgandolfi.dev.flashcards.infrastructure.security.JwtService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TagController.class)
@AutoConfigureMockMvc(addFilters = false)
class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TagService tagService;

    @MockitoBean
    private JwtService jwtService;

    private final UUID tagId = UUID.randomUUID();

    @Test
    void listReturnsTagsWithCounts() throws Exception {
        when(tagService.list()).thenReturn(List.of(
                new TagResponse(tagId, "Java", 3L),
                new TagResponse(UUID.randomUUID(), "Spring", 0L)));

        mockMvc.perform(get("/api/v1/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Java"))
                .andExpect(jsonPath("$[0].cardsCount").value(3));
    }

    @Test
    void createReturnsCreated() throws Exception {
        when(tagService.create(any())).thenReturn(new TagResponse(tagId, "Java", 0L));

        mockMvc.perform(post("/api/v1/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Java\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(tagId.toString()))
                .andExpect(jsonPath("$.name").value("Java"));
    }

    @Test
    void createRejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/v1/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createDuplicateReturnsConflict() throws Exception {
        when(tagService.create(any())).thenThrow(new ConflictException("Tag ja existe: Java"));

        mockMvc.perform(post("/api/v1/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Java\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/tags/" + tagId))
                .andExpect(status().isNoContent());
    }
}
