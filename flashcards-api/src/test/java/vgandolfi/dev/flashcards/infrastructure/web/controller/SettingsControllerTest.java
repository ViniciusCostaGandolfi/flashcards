package vgandolfi.dev.flashcards.infrastructure.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vgandolfi.dev.flashcards.application.dto.response.SettingsResponse;
import vgandolfi.dev.flashcards.application.service.SettingsService;
import vgandolfi.dev.flashcards.infrastructure.security.JwtService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SettingsController.class)
@AutoConfigureMockMvc(addFilters = false)
class SettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SettingsService settingsService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void getReturnsSettings() throws Exception {
        when(settingsService.get()).thenReturn(new SettingsResponse("reader", "generator",
                "prompt", 6, "pt-BR", "native"));

        mockMvc.perform(get("/api/v1/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readerModel").value("reader"))
                .andExpect(jsonPath("$.cardsPerGeneration").value(6))
                .andExpect(jsonPath("$.language").value("pt-BR"));
    }

    @Test
    void updateReturnsUpdatedSettings() throws Exception {
        when(settingsService.update(any())).thenReturn(new SettingsResponse("reader", "novo",
                "prompt", 10, "pt-BR", "native"));

        mockMvc.perform(put("/api/v1/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"generatorModel\":\"novo\",\"cardsPerGeneration\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatorModel").value("novo"))
                .andExpect(jsonPath("$.cardsPerGeneration").value(10));
    }

    @Test
    void updateRejectsCardsPerGenerationBelowMinimum() throws Exception {
        mockMvc.perform(put("/api/v1/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cardsPerGeneration\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
