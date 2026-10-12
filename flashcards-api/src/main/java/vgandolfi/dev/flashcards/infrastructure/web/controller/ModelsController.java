package vgandolfi.dev.flashcards.infrastructure.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vgandolfi.dev.flashcards.application.dto.response.ModelResponse;

import java.util.List;

/**
 * Catalogo curado de modelos para os seletores da UI. Nao consulta o OpenRouter
 * em tempo real — lista apenas os modelos suportados.
 */
@RestController
@RequestMapping("/api/v1/models")
public class ModelsController {

    private static final List<ModelResponse> MODELS = List.of(
            new ModelResponse("google/gemini-2.5-flash", "Gemini 2.5 Flash (visao)",
                    List.of("text", "image", "file")),
            new ModelResponse("openai/gpt-5.6-luna", "GPT-5.6 Luna (visao)",
                    List.of("text", "image", "file")),
            new ModelResponse("deepseek/deepseek-v4.1-flash", "DeepSeek v4.1 Flash (texto)",
                    List.of("text")));

    @GetMapping
    public ResponseEntity<List<ModelResponse>> list() {
        return ResponseEntity.ok(MODELS);
    }
}
