package vgandolfi.dev.flashcards.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import vgandolfi.dev.flashcards.application.event.GenerationJobCreatedEvent;
import vgandolfi.dev.flashcards.domain.exception.BusinessException;
import vgandolfi.dev.flashcards.domain.model.AppSettings;
import vgandolfi.dev.flashcards.domain.model.Card;
import vgandolfi.dev.flashcards.domain.model.Deck;
import vgandolfi.dev.flashcards.domain.model.GenerationJob;
import vgandolfi.dev.flashcards.domain.model.SourceFile;
import vgandolfi.dev.flashcards.domain.model.User;
import vgandolfi.dev.flashcards.domain.model.enums.GenerationStatus;
import vgandolfi.dev.flashcards.domain.model.enums.SourceType;
import vgandolfi.dev.flashcards.domain.repository.CardRepository;
import vgandolfi.dev.flashcards.domain.repository.DeckRepository;
import vgandolfi.dev.flashcards.domain.repository.GenerationJobRepository;
import vgandolfi.dev.flashcards.domain.repository.SourceFileRepository;
import vgandolfi.dev.flashcards.domain.repository.UserRepository;
import vgandolfi.dev.flashcards.infrastructure.openrouter.GeneratedCard;
import vgandolfi.dev.flashcards.infrastructure.openrouter.OpenRouterClient;
import vgandolfi.dev.flashcards.infrastructure.openrouter.OpenRouterProperties;
import vgandolfi.dev.flashcards.infrastructure.s3.S3Storage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private SourceFileRepository sourceFileRepository;
    @Mock
    private GenerationJobRepository generationJobRepository;
    @Mock
    private DeckRepository deckRepository;
    @Mock
    private CardRepository cardRepository;
    @Mock
    private SettingsService settingsService;
    @Mock
    private OpenRouterClient openRouterClient;
    @Mock
    private OpenRouterProperties openRouterProperties;
    @Mock
    private S3Storage s3Storage;
    @Mock
    private ApplicationEventPublisher events;

    @InjectMocks
    private GenerationService generationService;

    private final UUID jobId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    private AppSettings settings() {
        AppSettings settings = new AppSettings();
        settings.setReaderModel("reader-model");
        settings.setGeneratorModel("generator-model");
        settings.setBasePrompt("BASE_PROMPT");
        settings.setCardsPerGeneration(3);
        return settings;
    }

    private GenerationJob textJob() {
        GenerationJob job = new GenerationJob();
        job.setId(jobId);
        job.setSourceType(SourceType.TEXT);
        job.setInputText("material de exemplo");
        job.setStatus(GenerationStatus.PENDING);
        return job;
    }

    private GeneratedCard card(String type) {
        return new GeneratedCard(type, "pergunta " + type, List.of(), "resposta", "explicacao");
    }

    private List<GeneratedCard> fullCards() {
        return List.of(card("CLOZE"), card("TRUE_FALSE"), card("MULTIPLE_CHOICE"));
    }

    private void stubPersist() {
        when(generationJobRepository.save(any(GenerationJob.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deckRepository.save(any(Deck.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void processTextJobGeneratesDeckAndCards() {
        GenerationJob job = textJob();
        when(generationJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(settingsService.requireSettings()).thenReturn(settings());
        when(openRouterClient.generateCards(eq("generator-model"), anyString(), eq("material de exemplo"), eq(3)))
                .thenReturn(fullCards());
        stubPersist();

        generationService.process(jobId);

        assertThat(job.getStatus()).isEqualTo(GenerationStatus.DONE);
        assertThat(job.getDeck()).isNotNull();
        assertThat(job.getDeck().getTitle()).isEqualTo("material de exemplo");
        verify(cardRepository, times(3)).save(any(Card.class));
    }

    @Test
    void processRepairsMissingCardTypesWithFollowUpCall() {
        GenerationJob job = textJob();
        when(generationJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(settingsService.requireSettings()).thenReturn(settings());
        when(openRouterClient.generateCards(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(List.of(card("CLOZE"), card("TRUE_FALSE")))
                .thenReturn(List.of(card("MULTIPLE_CHOICE")));
        stubPersist();

        generationService.process(jobId);

        assertThat(job.getStatus()).isEqualTo(GenerationStatus.DONE);
        verify(openRouterClient, times(2)).generateCards(anyString(), anyString(), anyString(), anyInt());
        verify(cardRepository, times(3)).save(any(Card.class));
    }

    @Test
    void processFailsWhenTypesCannotBeRepaired() {
        GenerationJob job = textJob();
        when(generationJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(settingsService.requireSettings()).thenReturn(settings());
        when(openRouterClient.generateCards(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(List.of(card("CLOZE")))
                .thenReturn(List.of());
        when(generationJobRepository.save(any(GenerationJob.class))).thenAnswer(inv -> inv.getArgument(0));

        generationService.process(jobId);

        assertThat(job.getStatus()).isEqualTo(GenerationStatus.FAILED);
        assertThat(job.getErrorMessage()).isNotBlank();
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void processWithSourceFileRunsReadingThenGenerating() {
        GenerationJob job = new GenerationJob();
        job.setId(jobId);
        job.setSourceType(SourceType.PDF);
        SourceFile sourceFile = new SourceFile();
        sourceFile.setStorageKey("uploads/key.pdf");
        sourceFile.setContentType("application/pdf");
        sourceFile.setOriginalFilename("material.pdf");
        job.setSourceFile(sourceFile);

        when(generationJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(settingsService.requireSettings()).thenReturn(settings());
        when(s3Storage.downloadBytes("uploads/key.pdf")).thenReturn(new byte[] {1, 2, 3});
        when(openRouterClient.extractText(eq("reader-model"), any(), eq("application/pdf"), eq("material.pdf")))
                .thenReturn("texto extraido do pdf");
        when(openRouterClient.generateCards(eq("generator-model"), anyString(), eq("texto extraido do pdf"), eq(3)))
                .thenReturn(fullCards());
        stubPersist();

        generationService.process(jobId);

        assertThat(job.getStatus()).isEqualTo(GenerationStatus.DONE);
        assertThat(job.getDeck().getTitle()).isEqualTo("material");
        verify(s3Storage).downloadBytes("uploads/key.pdf");
    }

    @Test
    void processFailsWhenExtractionReturnsBlank() {
        GenerationJob job = new GenerationJob();
        job.setId(jobId);
        job.setSourceType(SourceType.IMAGE);
        SourceFile sourceFile = new SourceFile();
        sourceFile.setStorageKey("uploads/key.png");
        sourceFile.setContentType("image/png");
        sourceFile.setOriginalFilename("foto.png");
        job.setSourceFile(sourceFile);

        when(generationJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(settingsService.requireSettings()).thenReturn(settings());
        when(s3Storage.downloadBytes("uploads/key.png")).thenReturn(new byte[] {1});
        when(openRouterClient.extractText(anyString(), any(), anyString(), anyString())).thenReturn("   ");
        when(generationJobRepository.save(any(GenerationJob.class))).thenAnswer(inv -> inv.getArgument(0));

        generationService.process(jobId);

        assertThat(job.getStatus()).isEqualTo(GenerationStatus.FAILED);
        assertThat(job.getErrorMessage()).contains("extrair texto");
    }

    @Test
    void createWithTextPersistsJobAndPublishesEvent() {
        User user = new User();
        user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(generationJobRepository.save(any(GenerationJob.class))).thenAnswer(inv -> {
            GenerationJob saved = inv.getArgument(0);
            saved.setId(jobId);
            return saved;
        });

        UUID result = generationService.create(userId, null, null, null, "texto livre", "Meu deck");

        assertThat(result).isEqualTo(jobId);
        verify(s3Storage, never()).uploadBytes(anyString(), any(), anyString());
        verify(events).publishEvent(any(GenerationJobCreatedEvent.class));
    }

    @Test
    void createWithImageUploadsToStorage() {
        User user = new User();
        user.setId(userId);
        byte[] bytes = {1, 2, 3};
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(sourceFileRepository.save(any(SourceFile.class))).thenAnswer(inv -> {
            SourceFile saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        when(generationJobRepository.save(any(GenerationJob.class))).thenAnswer(inv -> {
            GenerationJob saved = inv.getArgument(0);
            saved.setId(jobId);
            return saved;
        });

        generationService.create(userId, "foto.png", "image/png", bytes, null, null);

        verify(s3Storage).uploadBytes(startsWith("uploads/" + userId + "/"), eq(bytes), eq("image/png"));
        verify(events).publishEvent(any(GenerationJobCreatedEvent.class));
    }

    @Test
    void createWithoutFileOrTextThrows() {
        User user = new User();
        user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> generationService.create(userId, null, null, null, null, null))
                .isInstanceOf(BusinessException.class);
    }
}
