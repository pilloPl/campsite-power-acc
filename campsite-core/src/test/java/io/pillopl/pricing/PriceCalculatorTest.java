package io.pillopl.pricing;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PriceCalculatorTest {

    static final LocalDateTime PLUG_IN = LocalDateTime.of(2026, 7, 6, 14, 0);

    static PricingInput supply(String kwh, long minutes) {
        return PricingInput.of(1L, null, PLUG_IN, PLUG_IN.plusMinutes(minutes), kwh != null ? new BigDecimal(kwh) : null,
                null);
    }

    @Test
    void energyIsRoundedUpToTheStepSizeInWh() throws Exception {
        PriceByEnergyCalculator calculator = new PriceByEnergyCalculator(supply("12.3401", 60),
                PriceComponent.energy("0.50", 100), null);

        BigDecimal cost = calculator.calculate();

        assertThat(calculator.getBilledQuantity()).isEqualByComparingTo("12.4");
        assertThat(cost).isEqualByComparingTo("6.20");
    }

    @Test
    void energyOnAStepBoundaryIsNotRoundedFurther() throws Exception {
        PriceByEnergyCalculator calculator = new PriceByEnergyCalculator(supply("12.3", 60),
                PriceComponent.energy("0.50", 100), null);

        assertThat(calculator.calculate()).isEqualByComparingTo("6.15");
    }

    @Test
    void energyKeepsFullPrecisionUntilTheTotalIsRounded() throws Exception {
        // 3 significant digits (the old MathContext(3)) would give 46.6 instead of 46.55385
        PriceByEnergyCalculator calculator = new PriceByEnergyCalculator(supply("103.453", 60),
                PriceComponent.energy("0.45", 1), null);

        assertThat(calculator.calculate()).isEqualByComparingTo("46.55385");
    }

    @Test
    void energyShareOfTheWindowIsPriced() throws Exception {
        PriceByEnergyCalculator calculator = new PriceByEnergyCalculator(supply("10", 60),
                PriceComponent.energy("0.40", 1), 900L);

        assertThat(calculator.calculate()).isEqualByComparingTo("1.00");
        assertThat(calculator.getBilledQuantity()).isEqualByComparingTo("2.5");
    }

    @Test
    void negativeEnergyCannotBePriced() {
        PriceByEnergyCalculator calculator = new PriceByEnergyCalculator(supply("-1", 60),
                PriceComponent.energy("0.50", 1), null);

        assertThatThrownBy(calculator::calculate).isInstanceOf(PriceCalculationException.class)
                .hasMessageContaining("Negative");
    }

    @Test
    void missingEnergyCannotBePriced() {
        PriceByEnergyCalculator calculator = new PriceByEnergyCalculator(supply(null, 60),
                PriceComponent.energy("0.50", 1), null);

        assertThatThrownBy(calculator::calculate).hasMessage("no metered energy to price");
    }

    @Test
    void componentOfAnotherDimensionIsRejected() {
        PriceByEnergyCalculator calculator = new PriceByEnergyCalculator(supply("1", 60),
                PriceComponent.flat("1.00"), null);

        assertThatThrownBy(calculator::calculate).hasMessageContaining("does not fit this calculator");
    }

    @Test
    void componentWithoutStepSizeIsRejected() {
        PriceComponent energy = PriceComponent.energy("0.50", 1);
        energy.setStepSize(null);

        assertThatThrownBy(() -> new PriceByEnergyCalculator(supply("1", 60), energy, null).calculate())
                .hasMessage("tariff component has no step size");
    }

    @Test
    void startedQuarterOfAnHourIsChargedFully() throws Exception {
        PriceByTimeCalculator calculator = new PriceByTimeCalculator(supply("1", 61),
                PriceComponent.time("0.20", 900), null);

        BigDecimal cost = calculator.calculate();

        assertThat(calculator.getBilledQuantity()).isEqualByComparingTo("1.25");
        assertThat(cost).isEqualByComparingTo("0.25");
    }

    @Test
    void negativeDurationCannotBePriced() {
        PriceByTimeCalculator calculator = new PriceByTimeCalculator(supply("1", -5),
                PriceComponent.time("0.20", 1), null);

        assertThatThrownBy(calculator::calculate).isInstanceOf(PriceCalculationException.class);
    }

    @Test
    void nightsAreCountedFromTheMidnightsOfTheSupply() throws Exception {
        PricingInput threeNights = PricingInput.of(1L, null, PLUG_IN, PLUG_IN.plusDays(3).minusHours(3),
                new BigDecimal("20"), null);
        PriceByNightCalculator calculator = new PriceByNightCalculator(threeNights, PriceComponent.perNight("3.50"));

        assertThat(calculator.calculate()).isEqualByComparingTo("10.50");
        assertThat(calculator.getBilledQuantity()).isEqualByComparingTo("3");
    }

    @Test
    void nightsGivenByTheStayAreRoundedUpToTheStepSize() throws Exception {
        PricingInput fiveNights = PricingInput.of(1L, null, PLUG_IN, PLUG_IN.plusHours(1), BigDecimal.ONE, 5);
        PriceComponent weekly = new PriceComponent(TariffDimensionType.PER_NIGHT, new BigDecimal("2.00"), 7);

        assertThat(new PriceByNightCalculator(fiveNights, weekly).calculate()).isEqualByComparingTo("14.00");
    }

    @Test
    void flatFeeIsChargedOnce() throws Exception {
        PriceByFlatRateCalculator calculator = new PriceByFlatRateCalculator(supply("40", 600),
                PriceComponent.flat("1.50"));

        assertThat(calculator.calculate()).isEqualByComparingTo("1.50");
    }

    @Test
    void dayVisitCountsAsOneNightAndNoSupplyAsNone() {
        assertThat(PricingInput.nightsBetween(PLUG_IN, PLUG_IN.plusHours(2))).isEqualTo(1);
        assertThat(PricingInput.nightsBetween(PLUG_IN, PLUG_IN)).isZero();
        assertThat(PricingInput.nightsBetween(PLUG_IN, PLUG_IN.plusDays(2))).isEqualTo(2);
    }
}
