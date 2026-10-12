package vgandolfi.dev.flashcards.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vgandolfi.dev.flashcards.application.event.GenerationJobCreatedEvent;
import vgandolfi.dev.flashcards.domain.exception.BusinessException;
import vgandolfi.dev.flashcards.domain.model.AppSettings;
import vgandolfi.dev.flashcards.domain.model.Card;
import vgandolfi.dev.flashcards.domain.model.Deck;
import vgandolfi.dev.flashcards.domain.model.GenerationJob;
import vgandolfi.dev.flashcards.domain.model.SourceFile;
import vgandolfi.dev.flashcards.domain.model.Tag;
import vgandolfi.dev.flashcards.domain.model.User;
import vgandolfi.dev.flashcards.domain.model.enums.CardType;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Pipeline assincrona de geracao de flashcards.
 *
 * <p>{@link #create} persiste o {@code SourceFile} (se houver) e o
 * {@code GenerationJob} (PENDING) e publica um evento. O listener
 * {@link #onJobCreated} roda apos o commit, de forma assincrona, executando
 * READING → GENERATING → DONE (ou FAILED + {@code errorMessage}).</p>
 */
@Service
public class GenerationService {

    private static final Logger log = LoggerFactory.getLogger(GenerationService.class);

    private static final int MIN_CARDS = AppSettings.MIN_CARDS_PER_GENERATION;
    private static final int MAX_TITLE_LENGTH = 120;

    private final UserRepository userRepository;
    private final SourceFileRepository sourceFileRepository;
    private final GenerationJobRepository generationJobRepository;
    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final SettingsService settingsService;
    private final TagService tagService;
    private final OpenRouterClient openRouterClient;
    private final OpenRouterProperties openRouterProperties;
    private final S3Storage s3Storage;
    private final ApplicationEventPublisher events;

    public GenerationService(UserRepository userRepository,
                             SourceFileRepository sourceFileRepository,
                             GenerationJobRepository generationJobRepository,
                             DeckRepository deckRepository,
                             CardRepository cardRepository,
                             SettingsService settingsService,
                             TagService tagService,
                             OpenRouterClient openRouterClient,
                             OpenRouterProperties openRouterProperties,
                             S3Storage s3Storage,
                             ApplicationEventPublisher events) {
        this.userRepository = userRepository;
        this.sourceFileRepository = sourceFileRepository;
        this.generationJobRepository = generationJobRepository;
        this.deckRepository = deckRepository;
        this.cardRepository = cardRepository;
        this.settingsService = settingsService;
        this.tagService = tagService;
        this.openRouterClient = openRouterClient;
        this.openRouterProperties = openRouterProperties;
        this.s3Storage = s3Storage;
        this.events = events;
    }

    /**
     * Cria o job e agenda o processamento.
     *
     * @param userId      usuario autenticado
     * @param filename    nome original do arquivo (null em jobs text-only)
     * @param contentType MIME type do arquivo (null em jobs text-only)
     * @param bytes       conteudo do arquivo (null/vazio em jobs text-only)
     * @param text        texto informado (null quando ha arquivo)
     * @param title       titulo opcional do deck
     * @param tags        tags (nomes) aplicadas a TODOS os cards gerados (opcional)
     * @return id do job (PENDING)
     */
    @Transactional
    public UUID create(UUID userId, String filename, String contentType, byte[] bytes, String text, String title,
                       List<String> tags) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Usuario nao encontrado"));

        boolean hasFile = bytes != null && bytes.length > 0;
        boolean hasText = text != null && !text.isBlank();
        if (!hasFile && !hasText) {
            throw new BusinessException("Informe um arquivo (imagem/PDF) ou um texto para gerar os flashcards");
        }

        SourceFile sourceFile = null;
        SourceType sourceType;
        if (hasFile) {
            sourceType = resolveSourceType(contentType, filename);
            String key = buildStorageKey(userId, filename);
            s3Storage.uploadBytes(key, bytes, contentType);
            sourceFile = new SourceFile();
            sourceFile.setOriginalFilename(safeFilename(filename));
            sourceFile.setContentType(contentType);
            sourceFile.setSizeBytes(bytes.length);
            sourceFile.setStorageKey(key);
            sourceFile.setUploadedAt(Instant.now());
            sourceFile = sourceFileRepository.save(sourceFile);
        } else {
            sourceType = SourceType.TEXT;
        }

        GenerationJob job = new GenerationJob();
        job.setUser(user);
        job.setSourceFile(sourceFile);
        job.setSourceType(sourceType);
        job.setInputText(hasFile ? null : text);
        job.setTitle(trimToNull(title));
        job.setStatus(GenerationStatus.PENDING);
        GenerationJob saved = generationJobRepository.save(job);

        log.info("generation_job_created jobId={} sourceType={} userId={}", saved.getId(), sourceType, userId);
        events.publishEvent(new GenerationJobCreatedEvent(saved.getId(),
                tags == null ? List.of() : List.copyOf(tags)));
        return saved.getId();
    }

    /** Executa a pipeline depois do commit da transacao de criacao. */
    @Async("generationExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onJobCreated(GenerationJobCreatedEvent event) {
        process(event.jobId(), event.tags());
    }

    /** Overload sem tags (usado em testes / chamadas internas). */
    void process(UUID jobId) {
        process(jobId, List.of());
    }

    void process(UUID jobId, List<String> jobTags) {
        GenerationJob job = generationJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.warn("generation_job_missing jobId={}", jobId);
            return;
        }
        try {
            AppSettings settings = settingsService.requireSettings();
            String readerModel = firstNonBlank(settings.getReaderModel(), openRouterProperties.readerModel());
            String generatorModel = firstNonBlank(settings.getGeneratorModel(), openRouterProperties.generatorModel());
            String basePrompt = firstNonBlank(settings.getBasePrompt(), openRouterProperties.basePrompt());
            int minCards = Math.max(MIN_CARDS, settings.getCardsPerGeneration());

            String material = readMaterial(job, readerModel);
            if (material == null || material.isBlank()) {
                throw new BusinessException("Nao foi possivel extrair texto do material informado");
            }

            job.setStatus(GenerationStatus.GENERATING);
            generationJobRepository.save(job);

            List<GeneratedCard> cards = openRouterClient.generateCards(generatorModel, basePrompt, material, minCards);
            cards = repairMissingTypes(cards, generatorModel, basePrompt, material, minCards);
            validateCards(cards);

            Deck deck = persistDeck(job, readerModel, generatorModel, basePrompt, material, cards, jobTags);
            job.setDeck(deck);
            job.setStatus(GenerationStatus.DONE);
            generationJobRepository.save(job);
            log.info("generation_job_done jobId={} deckId={} cards={}", job.getId(), deck.getId(), cards.size());
        } catch (RuntimeException ex) {
            log.error("generation_job_failed jobId={} error={}", jobId, ex.getMessage());
            job.setStatus(GenerationStatus.FAILED);
            job.setErrorMessage(ex.getMessage());
            generationJobRepository.save(job);
        }
    }

    // ------------------------------------------------------------------
    // Pipeline steps
    // ------------------------------------------------------------------

    private String readMaterial(GenerationJob job, String readerModel) {
        if (job.getSourceFile() == null) {
            return job.getInputText();
        }
        job.setStatus(GenerationStatus.READING);
        generationJobRepository.save(job);
        SourceFile sourceFile = job.getSourceFile();
        byte[] bytes = s3Storage.downloadBytes(sourceFile.getStorageKey());
        return openRouterClient.extractText(readerModel, bytes, sourceFile.getContentType(),
                sourceFile.getOriginalFilename());
    }

    /**
     * Se algum tipo obrigatorio estiver ausente, faz uma chamada extra pedindo
     * apenas os tipos faltantes e mescla o resultado.
     */
    List<GeneratedCard> repairMissingTypes(List<GeneratedCard> cards, String generatorModel, String basePrompt,
                                           String material, int minCards) {
        List<GeneratedCard> merged = new ArrayList<>(cards == null ? List.of() : cards);
        EnumSet<CardType> missing = missingTypes(merged);
        if (missing.isEmpty()) {
            return merged;
        }
        String repairPrompt = basePrompt + "\nGere APENAS cartoes dos seguintes tipos: " + missing;
        List<GeneratedCard> extra = openRouterClient.generateCards(generatorModel, repairPrompt, material,
                Math.max(missing.size(), 1));
        if (extra != null) {
            merged.addAll(extra);
        }
        log.info("generation_repair requested={} recovered={}", missing, extra == null ? 0 : extra.size());
        return merged;
    }

    private void validateCards(List<GeneratedCard> cards) {
        List<GeneratedCard> valid = cards.stream().filter(c -> parseType(c.type()) != null).toList();
        if (valid.size() < MIN_CARDS) {
            throw new BusinessException("Geracao insuficiente: esperado ao menos " + MIN_CARDS
                    + " cartoes, obtido " + valid.size());
        }
        EnumSet<CardType> missing = missingTypes(valid);
        if (!missing.isEmpty()) {
            throw new BusinessException("Geracao incompleta: faltam cartoes do(s) tipo(s) " + missing);
        }
    }

    private Deck persistDeck(GenerationJob job, String readerModel, String generatorModel, String basePrompt,
                             String material, List<GeneratedCard> cards, List<String> jobTags) {
        Deck deck = new Deck();
        deck.setTitle(resolveTitle(job, material));
        deck.setSourceType(job.getSourceType());
        deck.setSourceFile(job.getSourceFile());
        deck.setExtractedText(material);
        deck.setReaderModel(readerModel);
        deck.setGeneratorModel(generatorModel);
        deck.setPromptUsed(basePrompt);
        Deck savedDeck = deckRepository.save(deck);

        // Tags do job aplicadas a TODOS os cards do deck (resolvidas uma vez).
        Set<Tag> deckTags = tagService.resolveTags(jobTags);

        int position = 0;
        for (GeneratedCard generated : cards) {
            CardType type = parseType(generated.type());
            if (type == null) {
                continue;
            }
            Card card = new Card();
            card.setDeck(savedDeck);
            card.setType(type);
            card.setQuestion(generated.question());
            card.setAnswer(generated.answer());
            card.setExplanation(generated.explanation());
            card.setPosition(position++);
            if (generated.options() != null) {
                card.setOptions(generated.options().stream().filter(o -> o != null && !o.isBlank()).toList());
            }
            // Tags do card = tags do job (deck) uniao tags sugeridas pelo modelo.
            Set<Tag> cardTags = new LinkedHashSet<>(deckTags);
            cardTags.addAll(tagService.resolveTags(generated.tags()));
            card.getTags().clear();
            card.getTags().addAll(cardTags);
            cardRepository.save(card);
        }
        return savedDeck;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private EnumSet<CardType> missingTypes(List<GeneratedCard> cards) {
        EnumSet<CardType> present = EnumSet.noneOf(CardType.class);
        for (GeneratedCard card : cards) {
            CardType type = parseType(card.type());
            if (type != null) {
                present.add(type);
            }
        }
        EnumSet<CardType> missing = EnumSet.allOf(CardType.class);
        missing.removeAll(present);
        return missing;
    }

    private CardType parseType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return CardType.valueOf(raw.trim().toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private SourceType resolveSourceType(String contentType, String filename) {
        if (contentType != null) {
            if (contentType.startsWith("image/")) {
                return SourceType.IMAGE;
            }
            if (contentType.equals("application/pdf")) {
                return SourceType.PDF;
            }
        }
        if (filename != null && filename.toLowerCase().endsWith(".pdf")) {
            return SourceType.PDF;
        }
        throw new BusinessException("Formato nao suportado: envie uma imagem ou um PDF");
    }

    private String buildStorageKey(UUID userId, String filename) {
        return "uploads/" + userId + "/" + UUID.randomUUID() + "-" + safeFilename(filename);
    }

    private String safeFilename(String filename) {
        String value = (filename == null || filename.isBlank()) ? "material" : filename;
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String resolveTitle(GenerationJob job, String material) {
        String title = trimToNull(job.getTitle());
        if (title != null) {
            return title;
        }
        SourceFile sourceFile = job.getSourceFile();
        if (sourceFile != null && sourceFile.getOriginalFilename() != null) {
            String name = sourceFile.getOriginalFilename();
            int dot = name.lastIndexOf('.');
            String base = dot > 0 ? name.substring(0, dot) : name;
            if (!base.isBlank()) {
                return base;
            }
        }
        if (material != null && !material.isBlank()) {
            String firstLine = material.strip().lines().findFirst().orElse("").strip();
            if (!firstLine.isBlank()) {
                return firstLine.length() > MAX_TITLE_LENGTH ? firstLine.substring(0, MAX_TITLE_LENGTH) : firstLine;
            }
        }
        return "Deck sem titulo";
    }

    private String firstNonBlank(String first, String fallback) {
        return first != null && !first.isBlank() ? first : fallback;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
