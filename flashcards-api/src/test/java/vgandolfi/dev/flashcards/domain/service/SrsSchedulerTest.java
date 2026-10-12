package vgandolfi.dev.flashcards.domain.service;

import org.junit.jupiter.api.Test;
import vgandolfi.dev.flashcards.domain.model.Card;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class SrsSchedulerTest {

    private final SrsScheduler scheduler = new SrsScheduler();
    private final Instant now = Instant.parse("2026-10-12T12:00:00Z");

    private Card card(double easeFactor, int intervalDays, int repetitions) {
        Card card = new Card();
        card.setEaseFactor(easeFactor);
        card.setIntervalDays(intervalDays);
        card.setRepetitions(repetitions);
        return card;
    }

    private LocalDate dueIn(int days) {
        return LocalDate.ofInstant(now, ZoneOffset.UTC).plusDays(days);
    }

    @Test
    void againResetsRepetitionsAndIntervalAndReducesEase() {
        Card card = card(2.5, 10, 4);

        scheduler.apply(card, ReviewGrade.AGAIN, now);

        assertThat(card.getRepetitions()).isZero();
        assertThat(card.getIntervalDays()).isEqualTo(1);
        assertThat(card.getEaseFactor()).isEqualTo(2.3);
        assertThat(card.getDueDate()).isEqualTo(dueIn(1));
        assertThat(card.getLastReviewedAt()).isEqualTo(now);
    }

    @Test
    void againNeverLowersEaseBelowMinimum() {
        Card card = card(1.4, 10, 4);

        scheduler.apply(card, ReviewGrade.AGAIN, now);

        assertThat(card.getEaseFactor()).isEqualTo(1.3);
    }

    @Test
    void hardGrowsIntervalGentlyAndReducesEase() {
        Card card = card(2.5, 10, 3);

        scheduler.apply(card, ReviewGrade.HARD, now);

        assertThat(card.getIntervalDays()).isEqualTo(12);
        assertThat(card.getRepetitions()).isEqualTo(3);
        assertThat(card.getEaseFactor()).isEqualTo(2.35);
        assertThat(card.getDueDate()).isEqualTo(dueIn(12));
    }

    @Test
    void hardKeepsIntervalAtLeastOneDay() {
        Card card = card(2.5, 0, 0);

        scheduler.apply(card, ReviewGrade.HARD, now);

        assertThat(card.getIntervalDays()).isEqualTo(1);
    }

    @Test
    void easyFirstReviewSchedulesOneDay() {
        Card card = card(2.5, 0, 0);

        scheduler.apply(card, ReviewGrade.EASY, now);

        assertThat(card.getRepetitions()).isEqualTo(1);
        assertThat(card.getIntervalDays()).isEqualTo(1);
        assertThat(card.getEaseFactor()).isEqualTo(2.65);
        assertThat(card.getDueDate()).isEqualTo(dueIn(1));
    }

    @Test
    void easySecondReviewSchedulesSixDays() {
        Card card = card(2.65, 1, 1);

        scheduler.apply(card, ReviewGrade.EASY, now);

        assertThat(card.getRepetitions()).isEqualTo(2);
        assertThat(card.getIntervalDays()).isEqualTo(6);
        assertThat(card.getEaseFactor()).isEqualTo(2.8);
    }

    @Test
    void easyLaterReviewMultipliesIntervalByEase() {
        Card card = card(2.8, 6, 2);

        scheduler.apply(card, ReviewGrade.EASY, now);

        assertThat(card.getRepetitions()).isEqualTo(3);
        assertThat(card.getIntervalDays()).isEqualTo(17); // round(6 * 2.8)
        assertThat(card.getEaseFactor()).isEqualTo(2.95);
        assertThat(card.getDueDate()).isEqualTo(dueIn(17));
    }
}
