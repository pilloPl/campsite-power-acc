package io.pillopl.pricing;

import io.pillopl.group.Market;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PriceCalculatorFactoryTest {

    // Monday
    static final LocalDateTime EVENING = LocalDateTime.of(2026, 7, 6, 20, 0);

    static PricingInput supply(LocalDateTime from, LocalDateTime to, String kwh) {
        return PricingInput.of(1L, null, from, to, new BigDecimal(kwh), null);
    }

    static Tariff dayNight() {
        Tariff tariff = new Tariff("DAY NIGHT", "day and night", Market.HR);
        tariff.addElement(new TariffElement().addComponent(PriceComponent.energy("0.30", 1))
                .restrictTo(TariffRestriction.hours(LocalTime.of(22, 0), LocalTime.of(6, 0))));
        tariff.addElement(new TariffElement().addComponent(PriceComponent.energy("0.50", 1))
                .addComponent(PriceComponent.flat("1.00")));
        return tariff;
    }

    static BigDecimal sum(List<PriceCalculator<PricingInput>> calculators) throws PriceCalculationException {
        BigDecimal total = BigDecimal.ZERO;
        for (PriceCalculator<PricingInput> calculator : calculators) {
            total = total.add(calculator.calculate());
        }
        return total;
    }

    @Test
    void supplyCrossingTheNightWindowIsSplitProportionallyToTime() throws Exception {
        // 20:00-02:00: 2 h day, 4 h night; 12 kWh -> 4 kWh at 0.50 + 8 kWh at 0.30 + flat 1.00
        List<PriceCalculator<PricingInput>> calculators = PriceCalculatorFactory.getCalculatorsForTariff(dayNight(),
                supply(EVENING, EVENING.plusHours(6), "12"));

        assertThat(calculators).extracting(c -> c.getPrice().getPrice().toPlainString())
                .containsExactly("0.50", "0.30", "1.00");
        assertThat(calculators.get(0).getShare()).isEqualByComparingTo(new BigDecimal("0.3333333333"));
        assertThat(sum(calculators)).isEqualByComparingTo("5.4000000000");
    }

    @Test
    void firstApplyingElementWinsForEachDimension() throws Exception {
        List<PriceCalculator<PricingInput>> calculators = PriceCalculatorFactory.getCalculatorsForTariff(dayNight(),
                supply(EVENING.withHour(23), EVENING.plusDays(1).withHour(1), "2"));

        assertThat(calculators).extracting(c -> c.getPrice().getPrice().toPlainString())
                .containsExactly("0.30", "1.00");
    }

    @Test
    void flatFeeComesFromTheElementApplyingAtPlugIn() throws Exception {
        Tariff tariff = new Tariff("WEEKEND FEE", "weekend fee", Market.HR);
        tariff.addElement(new TariffElement().addComponent(PriceComponent.flat("3.00"))
                .restrictTo(TariffRestriction.days(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)));
        tariff.addElement(new TariffElement().addComponent(PriceComponent.flat("1.00")));

        LocalDateTime saturday = LocalDateTime.of(2026, 7, 11, 10, 0);
        assertThat(sum(PriceCalculatorFactory.getCalculatorsForTariff(tariff,
                supply(saturday, saturday.plusDays(2), "5")))).isEqualByComparingTo("3.00");
        assertThat(sum(PriceCalculatorFactory.getCalculatorsForTariff(tariff,
                supply(EVENING, EVENING.plusHours(1), "5")))).isEqualByComparingTo("1.00");
    }

    @Test
    void highSeasonPriceAppliesOnlyInsideTheSeason() throws Exception {
        Tariff tariff = new Tariff("SEASONS", "high and low season", Market.HR);
        tariff.addElement(new TariffElement().addComponent(PriceComponent.energy("0.60", 1))
                .restrictTo(TariffRestriction.season(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 31))));
        tariff.addElement(new TariffElement().addComponent(PriceComponent.energy("0.40", 1)));

        // 31 August 12:00 to 1 September 12:00: half in the high season
        LocalDateTime lastDay = LocalDateTime.of(2026, 8, 31, 12, 0);
        assertThat(sum(PriceCalculatorFactory.getCalculatorsForTariff(tariff,
                supply(lastDay, lastDay.plusDays(1), "10")))).isEqualByComparingTo("5.00");
    }

    @Test
    void kwhRangeIsCheckedAgainstTheWholeSupply() throws Exception {
        Tariff tariff = new Tariff("BULK", "bulk discount", Market.HR);
        tariff.addElement(new TariffElement().addComponent(PriceComponent.energy("0.35", 1))
                .restrictTo(TariffRestriction.kwhRange(new BigDecimal("50"), null)));
        tariff.addElement(new TariffElement().addComponent(PriceComponent.energy("0.50", 1)));

        assertThat(sum(PriceCalculatorFactory.getCalculatorsForTariff(tariff,
                supply(EVENING, EVENING.plusDays(3), "60")))).isEqualByComparingTo("21.00");
        assertThat(sum(PriceCalculatorFactory.getCalculatorsForTariff(tariff,
                supply(EVENING, EVENING.plusDays(1), "10")))).isEqualByComparingTo("5.00");
    }

    @Test
    void sliceWithoutApplyingElementIsNotCharged() throws Exception {
        Tariff tariff = new Tariff("NIGHT PARKING", "time charged at night only", Market.HR);
        tariff.addElement(new TariffElement().addComponent(PriceComponent.time("1.00", 1))
                .restrictTo(TariffRestriction.hours(LocalTime.of(22, 0), LocalTime.of(6, 0))));

        assertThat(sum(PriceCalculatorFactory.getCalculatorsForTariff(tariff,
                supply(EVENING, EVENING.plusHours(4), "0")))).isEqualByComparingTo("2.00");
    }

    @Test
    void supplyWithoutDurationGetsTheWholeEnergyAtPlugIn() throws Exception {
        assertThat(sum(PriceCalculatorFactory.getCalculatorsForTariff(dayNight(), supply(EVENING, EVENING, "1"))))
                .isEqualByComparingTo("1.50");
    }

    @Test
    void supplyEndingBeforeItsStartCannotBePriced() {
        assertThatThrownBy(() -> PriceCalculatorFactory.getCalculatorsForTariff(dayNight(),
                supply(EVENING, EVENING.minusHours(1), "1"))).isInstanceOf(PriceCalculationException.class);
    }

    @Test
    void supplyWithoutPlugOutCannotBePriced() {
        assertThatThrownBy(() -> PriceCalculatorFactory.getCalculatorsForTariff(dayNight(),
                PricingInput.of(1L, null, EVENING, null, BigDecimal.ONE, null)))
                .hasMessageContaining("plug-out");
    }
}
