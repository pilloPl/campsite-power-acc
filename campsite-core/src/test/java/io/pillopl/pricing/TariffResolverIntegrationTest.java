package io.pillopl.pricing;

import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.group.Market;
import io.pillopl.group.attribute.DefaultTariffService;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.Socket;
import io.pillopl.persistence.dao.TariffApplicabilityDao;
import io.pillopl.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class TariffResolverIntegrationTest extends IntegrationTest {

    static final LocalDateTime JULY = LocalDateTime.of(2026, 7, 1, 0, 0);

    @Autowired TariffResolver tariffResolver;
    @Autowired TariffApplicabilityDao tariffApplicabilityDao;
    @Autowired DefaultTariffService defaultTariffService;
    @Autowired PricingFixture pricing;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired GroupManager groupManager;
    @Autowired GroupService groupService;

    Group campsite, section;
    Pedestal pedestal;
    Socket socket;

    @BeforeEach
    void setUp() {
        campsite = groupManager.createChildGroup(groupService.fetchSiteGroup("HR SITES"), "BAY CAMPSITE", "Bay");
        section = groupManager.createChildGroup(campsite, "BAY BEACH SECTION", "Beach");
        pedestal = infrastructure.pedestal("HR-BAY-P001", section, 2);
        socket = pedestal.getSockets().get(0);
    }

    @Test
    void socketWithoutAnyAssignmentGetsTheSeededMarketDefault() {
        ResolvedTariff resolved = tariffResolver.resolve(socket.getId(), null, JULY);

        assertThat(resolved.source()).isEqualTo(TariffSource.MARKET_DEFAULT);
        assertThat(resolved.tariff().getName()).isEqualTo("HR STANDARD");
    }

    @Test
    void eachMoreSpecificStepWinsOverTheOneBelow() {
        defaultTariffService.setDefaultTariff(campsite, pricing.energyTariff("BAY", Market.HR, "0.42"));
        assertThat(tariffResolver.resolve(socket, null, JULY).source()).isEqualTo(TariffSource.CAMPSITE_DEFAULT);

        tariffApplicabilityDao.saveAndFlush(TariffApplicability.forPedestal(pedestal,
                pricing.energyTariff("BEACH PEDESTAL", Market.HR, "0.48"), JULY.minusMonths(1)));
        assertThat(tariffResolver.resolve(socket, null, JULY).source()).isEqualTo(TariffSource.PEDESTAL);

        tariffApplicabilityDao.saveAndFlush(TariffApplicability.forSocket(socket,
                pricing.energyTariff("32A SOCKET", Market.HR, "0.55"), JULY.minusMonths(1)));
        ResolvedTariff resolved = tariffResolver.resolve(socket, null, JULY);
        assertThat(resolved.source()).isEqualTo(TariffSource.SOCKET);
        assertThat(resolved.tariff().getName()).isEqualTo("32A SOCKET");

        Socket otherSocket = pedestal.getSockets().get(1);
        assertThat(tariffResolver.resolve(otherSocket, null, JULY).source()).isEqualTo(TariffSource.PEDESTAL);
    }

    @Test
    void assignmentStartingAfterThePlugInDoesNotPriceTheSupply() {
        tariffApplicabilityDao.saveAndFlush(TariffApplicability.forSocket(socket,
                pricing.energyTariff("FROM AUGUST", Market.HR, "0.60"), JULY.plusMonths(1)));

        assertThat(tariffResolver.resolve(socket, null, JULY).source()).isEqualTo(TariffSource.MARKET_DEFAULT);
        assertThat(tariffResolver.resolve(socket, null, JULY.plusMonths(1)).source()).isEqualTo(TariffSource.SOCKET);
    }
}
