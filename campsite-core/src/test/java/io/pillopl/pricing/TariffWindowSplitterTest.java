package io.pillopl.pricing;

import io.pillopl.group.Market;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TariffWindowSplitterTest {

    static final LocalDateTime EVENING = LocalDateTime.of(2026, 7, 6, 20, 0);

    static Tariff nightTariff() {
        Tariff tariff = new Tariff("NIGHT", "night", Market.HR);
        tariff.addElement(new TariffElement().addComponent(PriceComponent.energy("0.30", 1))
                .restrictTo(TariffRestriction.hours(LocalTime.of(22, 0), LocalTime.of(6, 0))));
        tariff.addElement(new TariffElement().addComponent(PriceComponent.energy("0.50", 1)));
        return tariff;
    }

    @Test
    void supplyIsCutAtWindowHoursAndMidnight() {
        List<TariffSlice> slices = TariffWindowSplitter.split(nightTariff(), EVENING, EVENING.plusHours(12));

        assertThat(slices).extracting(TariffSlice::start).containsExactly(EVENING, EVENING.withHour(22),
                EVENING.plusDays(1).withHour(0), EVENING.plusDays(1).withHour(6));
        assertThat(slices).extracting(TariffSlice::seconds).containsExactly(7200L, 7200L, 21600L, 7200L);
    }

    @Test
    void supplyInsideOneWindowAndOneDayStaysWhole() {
        List<TariffSlice> slices = TariffWindowSplitter.split(nightTariff(), EVENING.withHour(8), EVENING);

        assertThat(slices).hasSize(1);
        assertThat(slices.get(0).seconds()).isEqualTo(12 * 3600L);
    }

    @Test
    void supplyWithoutDurationIsOneEmptySlice() {
        assertThat(TariffWindowSplitter.split(nightTariff(), EVENING, EVENING)).containsExactly(
                new TariffSlice(EVENING, EVENING));
    }

    @Test
    void supplyEndingBeforeItsStartCannotBeSplit() {
        assertThatThrownBy(() -> TariffWindowSplitter.split(nightTariff(), EVENING, EVENING.minusMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
