package io.pillopl.pricing;

import io.pillopl.group.Market;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class TariffPriceTextTest {

    @Test
    void elementsAreWrittenInOrderWithTheirWindows() {
        Tariff tariff = new Tariff("DAY NIGHT", "day and night", Market.HR);
        tariff.addElement(new TariffElement().addComponent(PriceComponent.energy("0.3", 1))
                .restrictTo(TariffRestriction.hours(LocalTime.of(22, 0), LocalTime.of(6, 0))));
        tariff.addElement(new TariffElement().addComponent(PriceComponent.energy("0.4550", 1))
                .addComponent(PriceComponent.flat("1")));

        assertThat(TariffPriceText.describe(tariff))
                .isEqualTo("22:00-06:00: 0.30 EUR/kWh; 0.455 EUR/kWh + 1.00 EUR/session");
    }

    @Test
    void seasonDaysAndKwhRangeAreWritten() {
        TariffRestriction restriction = TariffRestriction.season(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 31));
        restriction.getDaysOfWeek().add(DayOfWeek.SUNDAY);
        restriction.getDaysOfWeek().add(DayOfWeek.SATURDAY);
        restriction.setMinKwh(new BigDecimal("50.0000"));

        assertThat(TariffPriceText.describe(restriction)).isEqualTo("01.07.-31.08. sat,sun 50- kWh");
    }

    @Test
    void noTariffHasNoText() {
        assertThat(TariffPriceText.describe((Tariff) null)).isNull();
    }
}
