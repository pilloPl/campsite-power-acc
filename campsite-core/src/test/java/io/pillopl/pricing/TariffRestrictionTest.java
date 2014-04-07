package io.pillopl.pricing;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class TariffRestrictionTest {

    static final LocalDateTime SATURDAY_NOON = LocalDateTime.of(2026, 7, 4, 12, 0);

    @Test
    void emptyRestrictionAlwaysMatches() {
        assertThat(new TariffRestriction().matches(SATURDAY_NOON, new BigDecimal("999"))).isTrue();
    }

    @Test
    void hoursIncludeTheStartAndExcludeTheEnd() {
        TariffRestriction daytime = TariffRestriction.hours(LocalTime.of(8, 0), LocalTime.of(20, 0));

        assertThat(daytime.matches(SATURDAY_NOON.withHour(8), null)).isTrue();
        assertThat(daytime.matches(SATURDAY_NOON.withHour(20), null)).isFalse();
        assertThat(daytime.matches(SATURDAY_NOON.withHour(7).withMinute(59), null)).isFalse();
    }

    @Test
    void nightWindowWrapsOverMidnight() {
        TariffRestriction night = TariffRestriction.hours(LocalTime.of(22, 0), LocalTime.of(6, 0));

        assertThat(night.matches(SATURDAY_NOON.withHour(23), null)).isTrue();
        assertThat(night.matches(SATURDAY_NOON.withHour(2), null)).isTrue();
        assertThat(night.matches(SATURDAY_NOON.withHour(6), null)).isFalse();
        assertThat(night.matches(SATURDAY_NOON, null)).isFalse();
    }

    @Test
    void sameStartAndEndHourMeansTheWholeDay() {
        TariffRestriction allDay = TariffRestriction.hours(LocalTime.MIDNIGHT, LocalTime.MIDNIGHT);

        assertThat(allDay.matches(SATURDAY_NOON, null)).isTrue();
    }

    @Test
    void seasonIncludesItsFirstAndLastDay() {
        TariffRestriction highSeason = TariffRestriction.season(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 31));

        assertThat(highSeason.matches(LocalDateTime.of(2026, 7, 1, 0, 0), null)).isTrue();
        assertThat(highSeason.matches(LocalDateTime.of(2026, 8, 31, 23, 59), null)).isTrue();
        assertThat(highSeason.matches(LocalDateTime.of(2026, 9, 1, 0, 0), null)).isFalse();
        assertThat(highSeason.matches(LocalDateTime.of(2026, 6, 30, 23, 59), null)).isFalse();
    }

    @Test
    void openEndedSeasonRestrictsOneSideOnly() {
        TariffRestriction fromAugust = TariffRestriction.season(LocalDate.of(2026, 8, 1), null);

        assertThat(fromAugust.matches(LocalDateTime.of(2030, 1, 1, 0, 0), null)).isTrue();
        assertThat(fromAugust.matches(LocalDateTime.of(2026, 7, 31, 0, 0), null)).isFalse();
    }

    @Test
    void weekendRestrictionMatchesOnlyItsDays() {
        TariffRestriction weekend = TariffRestriction.days(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

        assertThat(weekend.matches(SATURDAY_NOON, null)).isTrue();
        assertThat(weekend.matches(SATURDAY_NOON.plusDays(2), null)).isFalse();
    }

    @Test
    void kwhRangeIncludesTheMinimumAndExcludesTheMaximum() {
        TariffRestriction firstTen = TariffRestriction.kwhRange(null, BigDecimal.TEN);
        TariffRestriction fromTen = TariffRestriction.kwhRange(BigDecimal.TEN, null);

        assertThat(firstTen.matches(SATURDAY_NOON, new BigDecimal("9.999"))).isTrue();
        assertThat(firstTen.matches(SATURDAY_NOON, BigDecimal.TEN)).isFalse();
        assertThat(fromTen.matches(SATURDAY_NOON, BigDecimal.TEN)).isTrue();
    }

    @Test
    void unknownSessionEnergySkipsTheKwhRange() {
        assertThat(TariffRestriction.kwhRange(BigDecimal.TEN, null).matches(SATURDAY_NOON, null)).isTrue();
    }

    @Test
    void allPartsMustMatchTogether() {
        TariffRestriction weekendNights = TariffRestriction.hours(LocalTime.of(22, 0), LocalTime.of(6, 0));
        weekendNights.getDaysOfWeek().add(DayOfWeek.SATURDAY);

        assertThat(weekendNights.matches(SATURDAY_NOON.withHour(23), null)).isTrue();
        assertThat(weekendNights.matches(SATURDAY_NOON, null)).isFalse();
        assertThat(weekendNights.matches(SATURDAY_NOON.plusDays(1).withHour(23), null)).isFalse();
    }

    @Test
    void elementWithoutRestrictionAppliesAlways() {
        assertThat(new TariffElement().appliesAt(SATURDAY_NOON, BigDecimal.ONE)).isTrue();
        assertThat(new TariffElement().restrictTo(TariffRestriction.days(DayOfWeek.MONDAY))
                .appliesAt(SATURDAY_NOON, BigDecimal.ONE)).isFalse();
    }
}
