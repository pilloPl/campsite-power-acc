package io.pillopl.reporting;

import io.pillopl.reporting.ReportingService.TimeResolution;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Buckets of the accumulated kWh report: where a month, quarter or year starts, where the next one starts and how the
 * chart labels it.
 */
class ReportingPeriodTest {

    @Test
    void periodStartsOnTheFirstDayOfItsMonthQuarterOrYear() {
        LocalDate day = LocalDate.of(2026, 8, 17);

        assertThat(ReportingService.startOfPeriod(TimeResolution.MONTH, day)).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(ReportingService.startOfPeriod(TimeResolution.QUARTER, day)).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(ReportingService.startOfPeriod(TimeResolution.YEAR, day)).isEqualTo(LocalDate.of(2026, 1, 1));
    }

    @Test
    void lastDayOfAQuarterStillBelongsToIt() {
        assertThat(ReportingService.startOfPeriod(TimeResolution.QUARTER, LocalDate.of(2026, 3, 31)))
                .isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(ReportingService.startOfPeriod(TimeResolution.QUARTER, LocalDate.of(2026, 12, 31)))
                .isEqualTo(LocalDate.of(2026, 10, 1));
    }

    @Test
    void nextPeriodOfTheLastBucketOfTheYearStartsInTheNextYear() {
        assertThat(ReportingService.nextPeriod(TimeResolution.MONTH, LocalDate.of(2026, 12, 1)))
                .isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(ReportingService.nextPeriod(TimeResolution.QUARTER, LocalDate.of(2026, 10, 1)))
                .isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(ReportingService.nextPeriod(TimeResolution.YEAR, LocalDate.of(2026, 1, 1)))
                .isEqualTo(LocalDate.of(2027, 1, 1));
    }

    @Test
    void labelsPadTheMonthAndNumberTheQuarter() {
        assertThat(ReportingService.periodLabel(TimeResolution.MONTH, LocalDate.of(2026, 1, 1))).isEqualTo("2026_01");
        assertThat(ReportingService.periodLabel(TimeResolution.QUARTER, LocalDate.of(2026, 10, 1)))
                .isEqualTo("2026_Q4");
        assertThat(ReportingService.periodLabel(TimeResolution.YEAR, LocalDate.of(2026, 1, 1))).isEqualTo("2026");
    }
}
