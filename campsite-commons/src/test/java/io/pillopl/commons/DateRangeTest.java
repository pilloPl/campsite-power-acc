package io.pillopl.commons;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateRangeTest {

    @Test
    void dateOnlyEndCoversTheWholeLastDay() {
        DateRange range = DateRange.ofDays("2016-05-01", "2016-05-02");

        assertThat(range.from()).isEqualTo(LocalDateTime.of(2016, 5, 1, 0, 0));
        assertThat(range.to().toLocalTime()).isEqualTo(LocalTime.MAX);
        assertThat(range.contains(LocalDateTime.of(2016, 5, 2, 23, 59, 59))).isTrue();
        assertThat(range.contains(LocalDateTime.of(2016, 5, 3, 0, 0))).isFalse();
    }

    @Test
    void dateTimeEndsAreTakenAsGiven() {
        DateRange range = DateRange.ofDays("2016-05-01T10:00", "2016-05-01T12:00");

        assertThat(range.contains(LocalDateTime.of(2016, 5, 1, 12, 0))).isTrue();
        assertThat(range.contains(LocalDateTime.of(2016, 5, 1, 12, 1))).isFalse();
    }

    @Test
    void missingEndsAreOpen() {
        assertThat(DateRange.ofDays(null, " ").isUnbounded()).isTrue();
        assertThat(new DateRange(null, LocalDateTime.of(2016, 1, 1, 0, 0))
                .contains(LocalDateTime.of(1999, 1, 1, 0, 0))).isTrue();
    }

    @Test
    void startAfterEndAndUnreadableDatesAreRejected() {
        assertThatThrownBy(() -> DateRange.ofDays("2016-05-02", "2016-05-01"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DateUtils.parseStartOfDay("01.05.2016"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
