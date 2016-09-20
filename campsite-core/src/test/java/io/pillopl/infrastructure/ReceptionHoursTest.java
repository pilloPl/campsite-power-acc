package io.pillopl.infrastructure;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class ReceptionHoursTest {

    private final ReceptionHours morning = new ReceptionHours(DayOfWeek.SATURDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));

    @Test
    void receptionIsOpenFromTheStartUntilJustBeforeTheEnd() {
        assertThat(morning.isOpenAt(DayOfWeek.SATURDAY, LocalTime.of(8, 0))).isTrue();
        assertThat(morning.isOpenAt(DayOfWeek.SATURDAY, LocalTime.of(11, 59))).isTrue();
        assertThat(morning.isOpenAt(DayOfWeek.SATURDAY, LocalTime.of(12, 0))).isFalse();
        assertThat(morning.isOpenAt(DayOfWeek.SATURDAY, LocalTime.of(7, 59))).isFalse();
    }

    @Test
    void receptionIsClosedDuringTheLunchBreakBetweenTwoRowsOfTheSameDay() {
        Location location = new Location("Rovinj", "52210", "Obala", "12");
        location.addReceptionHours(morning);
        location.addReceptionHours(new ReceptionHours(DayOfWeek.SATURDAY, LocalTime.of(14, 0), LocalTime.of(18, 0)));

        assertThat(location.getReceptionHours()).noneMatch(h -> h.isOpenAt(DayOfWeek.SATURDAY, LocalTime.of(13, 0)));
        assertThat(location.getReceptionHours()).anyMatch(h -> h.isOpenAt(DayOfWeek.SATURDAY, LocalTime.of(15, 30)));
        assertThat(location.getReceptionHours()).allMatch(h -> h.getLocation() == location);
    }

    @Test
    void receptionHoursOfAnotherDayDoNotApply() {
        assertThat(morning.isOpenAt(DayOfWeek.SUNDAY, LocalTime.of(9, 0))).isFalse();
    }
}
