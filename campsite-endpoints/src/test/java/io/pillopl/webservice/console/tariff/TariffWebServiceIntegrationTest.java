package io.pillopl.webservice.console.tariff;

import io.pillopl.dto.pricing.PriceComponentDTO;
import io.pillopl.dto.pricing.TariffDTO;
import io.pillopl.dto.pricing.TariffElementDTO;
import io.pillopl.dto.pricing.TariffRestrictionDTO;
import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupType;
import io.pillopl.group.Market;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.pricing.PricingFixture;
import io.pillopl.pricing.Tariff;
import io.pillopl.user.UserroleName;
import io.pillopl.webservice.console.WebServiceIntegrationTest;
import io.pillopl.webservice.console.common.WebServicePaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TariffWebServiceIntegrationTest extends WebServiceIntegrationTest {

    static final String PATH = WebServicePaths.TARIFF;

    static final LocalDateTime EVENING = LocalDateTime.of(2026, 7, 6, 20, 0);

    @Autowired GroupManager groupManager;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired PricingFixture pricing;

    Group bay, cove;
    Pedestal atBay, atCove;
    Long baySocket;

    @BeforeEach
    void setUp() {
        bay = groupManager.createChildGroup(console.site("HR SITES"), "BAY CAMPSITE", "Bay", GroupType.SITE);
        cove = groupManager.createChildGroup(console.site("HR SITES"), "COVE CAMPSITE", "Cove", GroupType.SITE);
        console.hrAdmin("admin");
        console.user("manager", UserroleName.CAMPSITE_MANAGER, console.staff("HR RECEPTION"),
                console.staff("HR RECEPTION"), bay);
        console.user("reception", UserroleName.RECEPTION, console.staff("HR RECEPTION"), console.staff("HR RECEPTION"),
                bay);
        atBay = infrastructure.pedestal("HR-BAY-P001", bay, 2);
        atCove = infrastructure.pedestal("HR-COVE-P001", cove, 1);
        baySocket = atBay.getSockets().get(0).getId();
    }

    static TariffDTO dayNight(String name) {
        TariffRestrictionDTO night = new TariffRestrictionDTO(LocalTime.of(22, 0), LocalTime.of(6, 0), null, null,
                null, null, null);
        return new TariffDTO(null, name, "day and night", null, null, false, null, null, List.of(
                new TariffElementDTO(null, 0, List.of(new PriceComponentDTO(null, "ENERGY", new BigDecimal("0.30"),
                        1, null)), night),
                new TariffElementDTO(null, 1, List.of(new PriceComponentDTO(null, "ENERGY", new BigDecimal("0.50"),
                        1, null), new PriceComponentDTO(null, "FLAT", new BigDecimal("1.00"), 1, null)), null)));
    }

    @Test
    void managerCreatesATariffOfTheMarketOnce() throws Exception {
        call("manager", PATH + "/saveTariff", new SaveTariffRequest(dayNight("BAY DAY NIGHT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tariff.market").value("HR"))
                .andExpect(jsonPath("$.tariff.priceText")
                        .value("22:00-06:00: 0.30 EUR/kWh; 0.50 EUR/kWh + 1.00 EUR/session"));
        call("reception", PATH + "/existsTariff", new ExistsTariffRequest("BAY DAY NIGHT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));
        call("manager", PATH + "/saveTariff", new SaveTariffRequest(dayNight("BAY DAY NIGHT")))
                .andExpect(status().isConflict());
        call("reception", PATH + "/saveTariff", new SaveTariffRequest(dayNight("RECEPTION")))
                .andExpect(status().isForbidden());
    }

    @Test
    void tariffListShowsOnlyTheCallersMarket() throws Exception {
        call("reception", PATH + "/findTariffs", new FindTariffsRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tariffs[*].name").value(hasItem("HR STANDARD")))
                .andExpect(jsonPath("$.tariffs[*].name").value(not(hasItem("IT STANDARD"))));
        call("reception", PATH + "/getDefaultTariff", new GetDefaultTariffRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tariff.name").value("HR STANDARD"));
        call("reception", PATH + "/getTariff", new GetTariffRequest(2L))
                .andExpect(status().isForbidden());
        call("reception", PATH + "/getTariff", new GetTariffRequest(999_999L))
                .andExpect(status().isNotFound());
        call("reception", PATH + "/getTariff", new GetTariffRequest(null))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidTariffIsAWrongRequest() throws Exception {
        TariffDTO noElements = new TariffDTO(null, "EMPTY", "empty", null, null, false, null, null, List.of());
        TariffDTO unknownDimension = new TariffDTO(null, "PARKING", "x", null, null, false, null, null, List.of(
                new TariffElementDTO(null, 0, List.of(new PriceComponentDTO(null, "PARKING", BigDecimal.ONE, 1,
                        null)), null)));

        call("manager", PATH + "/saveTariff", new SaveTariffRequest(noElements)).andExpect(status().isBadRequest());
        call("manager", PATH + "/saveTariff", new SaveTariffRequest(unknownDimension))
                .andExpect(status().isBadRequest());
    }

    @Test
    void socketGetsAPedestalTariffAndThePriceFollowsIt() throws Exception {
        Tariff dayNight = pricing.dayNightTariff("BAY DAY NIGHT", Market.HR, "0.50", "0.30", "1.00");

        call("reception", PATH + "/findSocketTariff", new FindSocketTariffRequest(baySocket, EVENING))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.socketTariff.source").value("MARKET_DEFAULT"));
        call("manager", PATH + "/applyTariffToPedestals", new ApplyTariffToPedestalsRequest(List.of("HR-BAY-P001"),
                dayNight.getId(), EVENING.minusDays(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignments", hasSize(1)))
                .andExpect(jsonPath("$.assignments[0].createdBy").value("manager"));
        call("reception", PATH + "/findSocketTariff", new FindSocketTariffRequest(baySocket, EVENING))
                .andExpect(jsonPath("$.socketTariff.source").value("PEDESTAL"))
                .andExpect(jsonPath("$.socketTariff.tariff.name").value("BAY DAY NIGHT"))
                .andExpect(jsonPath("$.socketTariff.assignments", hasSize(1)));
        call("reception", PATH + "/simulatePrice", new SimulatePriceRequest(baySocket, null, EVENING,
                EVENING.plusHours(6), new BigDecimal("12"), null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulation.status").value("PRICED"))
                .andExpect(jsonPath("$.simulation.cost").value(5.40))
                .andExpect(jsonPath("$.simulation.costText").value("5.40 EUR"))
                .andExpect(jsonPath("$.simulation.lines", hasSize(3)));
    }

    @Test
    void simulationWithAGivenTariffAndWrongInputs() throws Exception {
        Tariff flat = pricing.energyTariff("BAY FLAT", Market.HR, "0.40");

        call("reception", PATH + "/simulatePrice", new SimulatePriceRequest(baySocket, flat.getId(), EVENING,
                EVENING.plusHours(1), new BigDecimal("10"), null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulation.tariffSource").value("GIVEN"))
                .andExpect(jsonPath("$.simulation.cost").value(4.00));
        call("reception", PATH + "/simulatePrice", new SimulatePriceRequest(baySocket, null, EVENING,
                EVENING.minusHours(1), BigDecimal.ONE, null))
                .andExpect(status().isBadRequest());
        call("reception", PATH + "/simulatePrice", new SimulatePriceRequest(baySocket, null, EVENING,
                EVENING.plusHours(1), new BigDecimal("-1"), null))
                .andExpect(status().isBadRequest());
        call("reception", PATH + "/simulatePrice", new SimulatePriceRequest(atCove.getSockets().get(0).getId(), null,
                EVENING, EVENING.plusHours(1), BigDecimal.ONE, null))
                .andExpect(status().isNotFound());
    }

    @Test
    void assignmentsOutsideTheScopeOrOfAnotherMarketAreRefused() throws Exception {
        Tariff bayTariff = pricing.energyTariff("BAY", Market.HR, "0.40");

        call("manager", PATH + "/applyTariffToPedestals", new ApplyTariffToPedestalsRequest(List.of("HR-COVE-P001"),
                bayTariff.getId(), EVENING)).andExpect(status().isForbidden());
        call("manager", PATH + "/applyTariffToPedestals", new ApplyTariffToPedestalsRequest(List.of("HR-NOPE"),
                bayTariff.getId(), EVENING)).andExpect(status().isNotFound());
        call("manager", PATH + "/applyTariffToSocket", new ApplyTariffToSocketRequest(baySocket, bayTariff.getId(),
                null)).andExpect(status().isBadRequest());
        call("manager", PATH + "/applyTariffToSocket", new ApplyTariffToSocketRequest(baySocket, 2L, EVENING))
                .andExpect(status().isForbidden());
        call("reception", PATH + "/applyTariffToSocket", new ApplyTariffToSocketRequest(baySocket, bayTariff.getId(),
                EVENING)).andExpect(status().isForbidden());
    }

    @Test
    void socketTariffIsAssignedAndItsPlannedAssignmentRemoved() throws Exception {
        Tariff bayTariff = pricing.energyTariff("BAY", Market.HR, "0.40");

        call("manager", PATH + "/applyTariffToSocket", new ApplyTariffToSocketRequest(baySocket, bayTariff.getId(),
                EVENING))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignment.tariffName").value("BAY"))
                .andExpect(jsonPath("$.removed").value(false));
        call("manager", PATH + "/applyTariffToSocket", new ApplyTariffToSocketRequest(baySocket, null, EVENING))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.removed").value(true));
    }

    @Test
    void campsiteDefaultTariffIsSetForSiteGroupsOfTheScope() throws Exception {
        Tariff bayTariff = pricing.energyTariff("BAY", Market.HR, "0.40");

        call("manager", PATH + "/setCampsiteDefaultTariff", new SetCampsiteDefaultTariffRequest(bay.getId(),
                bayTariff.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.effectiveDefaultTariff.name").value("BAY"));
        call("reception", PATH + "/findSocketTariff", new FindSocketTariffRequest(baySocket, null))
                .andExpect(jsonPath("$.socketTariff.source").value("CAMPSITE_DEFAULT"));
        call("manager", PATH + "/setCampsiteDefaultTariff", new SetCampsiteDefaultTariffRequest(cove.getId(),
                bayTariff.getId())).andExpect(status().isForbidden());
        call("manager", PATH + "/setCampsiteDefaultTariff", new SetCampsiteDefaultTariffRequest(console.staff("HR RECEPTION").getId(), bayTariff.getId())).andExpect(status().isBadRequest());
        call("manager", PATH + "/setCampsiteDefaultTariff", new SetCampsiteDefaultTariffRequest(999_999L,
                bayTariff.getId())).andExpect(status().isNotFound());
        call("manager", PATH + "/setCampsiteDefaultTariff", new SetCampsiteDefaultTariffRequest(bay.getId(), null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.effectiveDefaultTariff").doesNotExist());
    }

    @Test
    void onlyTheMarketAdminMovesTheMarketDefault() throws Exception {
        Tariff bayTariff = pricing.energyTariff("BAY", Market.HR, "0.40");

        call("manager", PATH + "/setMarketDefaultTariff", new SetMarketDefaultTariffRequest(bayTariff.getId()))
                .andExpect(status().isForbidden());
        call("admin", PATH + "/setMarketDefaultTariff", new SetMarketDefaultTariffRequest(bayTariff.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tariff.defaultTariff").value(true));
    }

    @Test
    void usedTariffCannotBeChanged() throws Exception {
        call("admin", PATH + "/updateTariff", new UpdateTariffRequest(1L, dayNight("ignored")))
                .andExpect(status().isConflict());
        Tariff bayTariff = pricing.energyTariff("BAY", Market.HR, "0.40");
        call("admin", PATH + "/updateTariff", new UpdateTariffRequest(bayTariff.getId(), dayNight("ignored")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tariff.name").value("BAY"))
                .andExpect(jsonPath("$.tariff.elements", hasSize(2)));
    }

    @Test
    void affiliateOwnersPedestalsGetTheTariff() throws Exception {
        console.user("bay.owner", UserroleName.AFFILIATE_OWNER, console.staff("HR RECEPTION"), bay);
        Tariff bayTariff = pricing.energyTariff("BAY", Market.HR, "0.40");

        call("admin", PATH + "/applyTariffToAffiliatePedestals", new ApplyTariffToAffiliatePedestalsRequest("bay.owner", bayTariff.getId(), EVENING))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignments", hasSize(1)))
                .andExpect(jsonPath("$.assignments[0].pedestalCode").value("HR-BAY-P001"));
        call("admin", PATH + "/applyTariffToAffiliatePedestals", new ApplyTariffToAffiliatePedestalsRequest("nobody", bayTariff.getId(), EVENING)).andExpect(status().isNotFound());
    }
}
