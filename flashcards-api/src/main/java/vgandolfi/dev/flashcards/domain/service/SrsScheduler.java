package vgandolfi.dev.flashcards.domain.service;

import vgandolfi.dev.flashcards.domain.model.Card;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Agendador SRS baseado no algoritmo SM-2 (versao simplificada).
 *
 * <p>Sem dependencia de framework: e um servico de dominio puro, aplicado pelo
 * {@code CardService} a cada revisao. Alem dos campos classicos do SM-2
 * ({@code easeFactor}, {@code intervalDays}, {@code repetitions}), atualiza
 * {@code dueDate} e {@code lastReviewedAt}.</p>
 *
 * <p>Regras (notas {@link ReviewGrade}):</p>
 * <ul>
 *   <li>{@code AGAIN}: repeticoes zeradas, intervalo = 1 dia, EF -= 0.2 (min 1.3);</li>
 *   <li>{@code HARD}: intervalo = max(1, round(intervalo * 1.2)), EF -= 0.15 (min 1.3),
 *       repeticoes inalteradas;</li>
 *   <li>{@code EASY}: repeticoes += 1; intervalo = 1 (1a), 6 (2a), ou
 *       round(intervalo * EF) nas demais; EF += 0.15.</li>
 * </ul>
 */
public class SrsScheduler {

    private static final double MIN_EASE_FACTOR = 1.3;
    private static final double EASY_BONUS = 0.15;
    private static final double HARD_PENALTY = 0.15;
    private static final double AGAIN_PENALTY = 0.2;

    /**
     * Aplica a nota de revisao ao cartao, mutando seus campos de SRS.
     *
     * @param card  cartao revisado (nao nulo)
     * @param grade nota atribuida pelo usuario (nao nula)
     * @param now   instante da revisao
     * @return o proprio cartao, para encadeamento
     */
    public Card apply(Card card, ReviewGrade grade, Instant now) {
        double easeFactor = card.getEaseFactor();
        int intervalDays = card.getIntervalDays();
        int repetitions = card.getRepetitions();

        switch (grade) {
            case AGAIN -> {
                repetitions = 0;
                intervalDays = 1;
                easeFactor = max(MIN_EASE_FACTOR, easeFactor - AGAIN_PENALTY);
            }
            case HARD -> {
                intervalDays = Math.max(1, (int) Math.round(intervalDays * 1.2));
                easeFactor = max(MIN_EASE_FACTOR, easeFactor - HARD_PENALTY);
            }
            case EASY -> {
                repetitions = repetitions + 1;
                if (repetitions == 1) {
                    intervalDays = 1;
                } else if (repetitions == 2) {
                    intervalDays = 6;
                } else {
                    intervalDays = Math.max(1, (int) Math.round(intervalDays * easeFactor));
                }
                easeFactor = easeFactor + EASY_BONUS;
            }
        }

        card.setEaseFactor(round2(easeFactor));
        card.setIntervalDays(intervalDays);
        card.setRepetitions(repetitions);
        card.setDueDate(LocalDate.ofInstant(now, ZoneOffset.UTC).plusDays(intervalDays));
        card.setLastReviewedAt(now);
        return card;
    }

    private double max(double a, double b) {
        return Math.max(a, b);
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
