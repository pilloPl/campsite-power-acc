package io.pillopl.commons;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateUtilsTest {

    @Test
    void blankOrMissingTextGivesNoDate() {
        assertThat(DateUtils.parseStartOfDay(null)).isNull();
        assertThat(DateUtils.parseEndOfDay("   ")).isNull();
    }

    @Test
    void dateOnlyTextIsWidenedToTheWholeDay() {
        assertThat(DateUtils.parseStartOfDay(" 2016-07-14 ")).isEqualTo(LocalDateTime.of(2016, 7, 14, 0, 0));
        assertThat(DateUtils.parseEndOfDay("2016-07-14")).isEqualTo(LocalDate.of(2016, 7, 14).atTime(LocalTime.MAX));
    }

    @Test
    void dateTimeTextIsKeptForBothEnds() {
        LocalDateTime checkIn = LocalDateTime.of(2016, 7, 14, 15, 30);

        assertThat(DateUtils.parseStartOfDay("2016-07-14T15:30")).isEqualTo(checkIn);
        assertThat(DateUtils.parseEndOfDay("2016-07-14T15:30")).isEqualTo(checkIn);
    }

    @Test
    void missingDayHasNoStartOrEnd() {
        assertThat(DateUtils.startOfDay(null)).isNull();
        assertThat(DateUtils.endOfDay(null)).isNull();
    }

    @Test
    void unreadableTextIsRejectedWithTheText() {
        assertThatThrownBy(() -> DateUtils.parseEndOfDay("2016-13-01"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2016-13-01");
    }
}
