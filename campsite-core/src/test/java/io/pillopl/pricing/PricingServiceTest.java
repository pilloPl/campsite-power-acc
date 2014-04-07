package io.pillopl.pricing;

import io.pillopl.group.Market;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PricingServiceTest {

    static final LocalDateTime PLUG_IN = LocalDateTime.of(2026, 7, 6, 20, 0);

    TariffResolver tariffResolver = mock(TariffResolver.class);
    PricingService pricingService = new PricingService(tariffResolver);

    static PricingInput supply(String kwh, int hours) {
        return PricingInput.of(41L, 7L, PLUG_IN, PLUG_IN.plusHours(hours), new BigDecimal(kwh), null);
    }

    static Tariff dayNight() {
        Tariff tariff = new Tariff("DAY NIGHT", "day and night", Market.HR);
        tariff.addElement(new TariffElement().addComponent(PriceComponent.energy("0.3333", 1))
                .restrictTo(TariffRestriction.hours(LocalTime.of(22, 0), LocalTime.of(6, 0))));
        tariff.addElement(new TariffElement().addComponent(PriceComponent.energy("0.5000", 1))
                .addComponent(PriceComponent.flat("1.00")));
        return tariff;
    }

    void resolvesTo(Tariff tariff, TariffSource source) {
        when(tariffResolver.resolve(eq(41L), eq(7L), any())).thenReturn(new ResolvedTariff(tariff, source));
    }

    @Test
    void supplyIsPricedWithTheResolvedTariffAndRoundedOnceToCents() {
        resolvesTo(dayNight(), TariffSource.PEDESTAL);

        // 20:00-02:00, 12 kWh: 4 kWh * 0.50 + 8 kWh * 0.3333 + 1.00 = 5.6664 -> 5.67
        PriceCalculation calculation = pricingService.calculateCost(supply("12", 6));

        assertThat(calculation.getStatus()).isEqualTo(PricingStatus.PRICED);
        assertThat(calculation.getCost()).isEqualByComparingTo("5.67");
        assertThat(calculation.getCost().scale()).isEqualTo(2);
        assertThat(calculation.getTariffSource()).isEqualTo(TariffSource.PEDESTAL);
        assertThat(calculation.getCurrency()).isEqualTo("EUR");
        assertThat(calculation.getLines()).extracting(PriceCalculationLine::dimensionType).containsExactly(
                TariffDimensionType.ENERGY, TariffDimensionType.ENERGY, TariffDimensionType.FLAT);
        assertThat(calculation.getLines().get(1).amount()).isEqualByComparingTo("2.6664");
    }

    @Test
    void halfCentIsRoundedUp() {
        Tariff tariff = new Tariff("HALF", "half cent", Market.HR);
        tariff.addElement(new TariffElement().addComponent(PriceComponent.energy("0.0050", 1)));
        resolvesTo(tariff, TariffSource.MARKET_DEFAULT);

        assertThat(pricingService.calculateCost(supply("1", 1)).getCost()).isEqualByComparingTo("0.01");
    }

    @Test
    void supplyThatIsNotBilledCostsNothingWithoutLookingForATariff() {
        PriceCalculation calculation = pricingService.calculateCost(
                PricingInput.notBillable(41L, 7L, PLUG_IN, PLUG_IN.plusHours(1), BigDecimal.TEN));

        assertThat(calculation.getStatus()).isEqualTo(PricingStatus.NOT_BILLABLE);
        assertThat(calculation.getCost()).isEqualByComparingTo("0.00");
        verifyNoInteractions(tariffResolver);
    }

    @Test
    void supplyWithoutSocketFailsWithoutCost() {
        PriceCalculation calculation = pricingService.calculateCost(
                PricingInput.of(null, 7L, PLUG_IN, PLUG_IN.plusHours(1), BigDecimal.TEN, null));

        assertThat(calculation.getStatus()).isEqualTo(PricingStatus.PRICING_FAILED);
        assertThat(calculation.getCost()).isNull();
        assertThat(calculation.getFailureReason()).isEqualTo("Socket is missing");
    }

    @Test
    void supplyWithoutPlugInFails() {
        PriceCalculation calculation = pricingService.calculateCost(
                PricingInput.of(41L, 7L, null, PLUG_IN, BigDecimal.TEN, null));

        assertThat(calculation.getFailureReason()).isEqualTo("Plug-in is missing");
    }

    @Test
    void socketWithoutTariffFailsWithTheReason() {
        when(tariffResolver.resolve(eq(41L), eq(7L), any())).thenThrow(new NoTariffFoundException("No tariff"));

        PriceCalculation calculation = pricingService.calculateCost(supply("1", 1));

        assertThat(calculation.getStatus()).isEqualTo(PricingStatus.PRICING_FAILED);
        assertThat(calculation.getFailureReason()).isEqualTo("No tariff");
        assertThat(calculation.getTariffName()).isNull();
    }

    @Test
    void negativeConsumptionFailsAndKeepsTheTariff() {
        resolvesTo(dayNight(), TariffSource.SOCKET);

        PriceCalculation calculation = pricingService.calculateCost(supply("-3", 1));

        assertThat(calculation.getStatus()).isEqualTo(PricingStatus.PRICING_FAILED);
        assertThat(calculation.getCost()).isNull();
        assertThat(calculation.getTariffName()).isEqualTo("DAY NIGHT");
    }

    @Test
    void negativeDurationFails() {
        resolvesTo(dayNight(), TariffSource.SOCKET);

        assertThat(pricingService.calculateCost(supply("1", -1)).getStatus())
                .isEqualTo(PricingStatus.PRICING_FAILED);
    }

    @Test
    void tariffWithoutAnyApplyingElementFails() {
        Tariff nightsOnly = new Tariff("NIGHTS ONLY", "nights only", Market.HR);
        nightsOnly.addElement(new TariffElement().addComponent(PriceComponent.flat("2.00"))
                .restrictTo(TariffRestriction.hours(LocalTime.of(0, 0), LocalTime.of(6, 0))));
        resolvesTo(nightsOnly, TariffSource.SOCKET);

        PriceCalculation calculation = pricingService.calculateCost(supply("1", 1));

        assertThat(calculation.getStatus()).isEqualTo(PricingStatus.PRICING_FAILED);
        assertThat(calculation.getFailureReason()).contains("applies");
    }

    @Test
    void givenTariffIsUsedAsItIs() {
        PriceCalculation calculation = pricingService.calculateCost(dayNight(), supply("2", 1));

        assertThat(calculation.getTariffSource()).isEqualTo(TariffSource.GIVEN);
        assertThat(calculation.getCost()).isEqualByComparingTo("2.00");
        verifyNoInteractions(tariffResolver);
    }

    @Test
    void missingGivenTariffFails() {
        assertThat(pricingService.calculateCost((Tariff) null, supply("2", 1)).getStatus())
                .isEqualTo(PricingStatus.PRICING_FAILED);
    }

    @Test
    void missingInputIsAProgrammingError() {
        assertThatThrownBy(() -> pricingService.calculateCost((PricingInput) null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
