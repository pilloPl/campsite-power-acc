package io.pillopl.pricing;

import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.group.Market;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.Socket;
import io.pillopl.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class PricingServiceIntegrationTest extends IntegrationTest {

    static final LocalDateTime EVENING = LocalDateTime.of(2026, 7, 6, 20, 0);

    @Autowired PricingService pricingService;
    @Autowired TariffApplicabilityService tariffApplicabilityService;
    @Autowired PricingFixture pricing;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired GroupManager groupManager;
    @Autowired GroupService groupService;

    Pedestal pedestal;
    Socket socket;

    @BeforeEach
    void setUp() {
        Group bay = groupManager.createChildGroup(groupService.fetchSiteGroup("HR SITES"), "BAY CAMPSITE", "Bay");
        pedestal = infrastructure.pedestal("HR-BAY-P001", bay, 1);
        socket = pedestal.getSockets().get(0);
    }

    PricingInput supply(String kwh, int hours) {
        return PricingInput.of(socket.getId(), null, EVENING, EVENING.plusHours(hours), new BigDecimal(kwh), null);
    }

    @Test
    void supplyWithoutAssignmentIsPricedWithTheMarketDefault() {
        PriceCalculation calculation = pricingService.calculateCost(supply("10.4567", 3));

        assertThat(calculation.getStatus()).isEqualTo(PricingStatus.PRICED);
        assertThat(calculation.getTariffName()).isEqualTo("HR STANDARD");
        // 10.457 kWh (1 Wh steps) * 0.45 = 4.70565
        assertThat(calculation.getCost()).isEqualByComparingTo("4.71");
    }

    @Test
    void pedestalTariffSplitsTheSupplyOverItsNightWindow() {
        Tariff dayNight = pricing.dayNightTariff("BAY DAY NIGHT", Market.HR, "0.50", "0.30", "1.00");
        tariffApplicabilityService.applyTariffToPedestal(pedestal.getId(), dayNight.getId(), EVENING.minusDays(1),
                "manager");

        PriceCalculation calculation = pricingService.calculateCost(supply("12", 6));

        assertThat(calculation.getTariffSource()).isEqualTo(TariffSource.PEDESTAL);
        assertThat(calculation.getCost()).isEqualByComparingTo("5.40");
        assertThat(pricingService.getPriceInformation(socket.getId(), null, EVENING))
                .isEqualTo("22:00-06:00: 0.30 EUR/kWh; 0.50 EUR/kWh + 1.00 EUR/session");
    }

    @Test
    void unknownSocketFailsWithoutCost() {
        PriceCalculation calculation = pricingService.calculateCost(
                PricingInput.of(999_999L, null, EVENING, EVENING.plusHours(1), BigDecimal.ONE, null));

        assertThat(calculation.getStatus()).isEqualTo(PricingStatus.PRICING_FAILED);
        assertThat(calculation.getCost()).isNull();
        assertThat(pricingService.getPriceInformation(999_999L, null, EVENING)).isNull();
    }
}
