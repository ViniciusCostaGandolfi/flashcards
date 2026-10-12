package vgandolfi.dev.flashcards.infrastructure.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vgandolfi.dev.flashcards.application.dto.response.CardResponse;
import vgandolfi.dev.flashcards.application.service.CardService;
import vgandolfi.dev.flashcards.domain.model.enums.CardType;
import vgandolfi.dev.flashcards.infrastructure.security.JwtService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CardController.class)
@AutoConfigureMockMvc(addFilters = false)
class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CardService cardService;

    @MockitoBean
    private JwtService jwtService;

    private final UUID cardId = UUID.randomUUID();
    private final UUID deckId = UUID.randomUUID();

    private CardResponse response(CardType type) {
        return new CardResponse(cardId, deckId, type, "Pergunta?", "Resposta", "expl", null, 0,
                List.of("A", "B"), 2.5, 0, 0, LocalDate.now(), null);
    }

    @Test
    void createReturnsCreated() throws Exception {
        when(cardService.create(any())).thenReturn(response(CardType.MULTIPLE_CHOICE));

        mockMvc.perform(post("/api/v1/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deckId\":\"" + deckId + "\",\"type\":\"MULTIPLE_CHOICE\","
                                + "\"question\":\"Pergunta?\",\"answer\":\"Resposta\","
                                + "\"options\":[\"A\",\"B\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(cardId.toString()))
                .andExpect(jsonPath("$.type").value("MULTIPLE_CHOICE"));
    }

    @Test
    void createRejectsMissingQuestion() throws Exception {
        mockMvc.perform(post("/api/v1/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deckId\":\"" + deckId + "\",\"type\":\"CLOZE\",\"answer\":\"a\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateReturnsUpdatedCard() throws Exception {
        when(cardService.update(eq(cardId), any())).thenReturn(response(CardType.TRUE_FALSE));

        mockMvc.perform(put("/api/v1/cards/" + cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"TRUE_FALSE\",\"question\":\"q\",\"answer\":\"Verdadeiro\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("TRUE_FALSE"));
    }

    @Test
    void reviewReturnsScheduledCard() throws Exception {
        when(cardService.review(eq(cardId), any())).thenReturn(response(CardType.CLOZE));

        mockMvc.perform(post("/api/v1/cards/" + cardId + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grade\":\"EASY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId.toString()));
    }

    @Test
    void reviewRejectsUnknownGrade() throws Exception {
        mockMvc.perform(post("/api/v1/cards/" + cardId + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grade\":\"IMPOSSIBLE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/cards/" + cardId))
                .andExpect(status().isNoContent());
    }
}
