package io.pillopl.value;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BillingPeriodTest {

    @Test
    void periodIsWrittenAsYearUnderscoreMonth() {
        BillingPeriod period = BillingPeriod.of(7, 2026);

        assertThat(period.getCode()).isEqualTo("2026_07");
        assertThat(BillingPeriod.parse("2026_07")).isEqualTo(period);
        assertThat(period.getFirstDay()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(period.getLastDay()).isEqualTo(LocalDate.of(2026, 7, 31));
        assertThat(period.getDays()).isEqualTo(31);
    }

    @Test
    void monthlyRunBillsTheMonthBeforeTheDay() {
        assertThat(BillingPeriod.before(LocalDate.of(2026, 1, 3)).getCode()).isEqualTo("2025_12");
    }

    @Test
    void monthOutsideTheCalendarOrAnotherFormIsRefused() {
        assertThatThrownBy(() -> BillingPeriod.of(13, 2026)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> BillingPeriod.parse("2026-07")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> BillingPeriod.parse("2026_13")).isInstanceOf(InvalidValueException.class);
    }
}
