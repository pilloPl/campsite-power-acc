package io.pillopl.pricing;

import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.Market;
import io.pillopl.group.attribute.DefaultTariffService;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.Socket;
import io.pillopl.persistence.dao.SocketDao;
import io.pillopl.persistence.dao.TariffApplicabilityDao;
import io.pillopl.persistence.dao.TariffDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TariffResolverTest {

    static final LocalDateTime AT = LocalDateTime.of(2026, 7, 6, 14, 0);

    SeasonalContractTariffProvider contracts = mock(SeasonalContractTariffProvider.class);
    TariffApplicabilityDao applicabilities = mock(TariffApplicabilityDao.class);
    DefaultTariffService campsiteDefaults = mock(DefaultTariffService.class);
    TariffDao tariffDao = mock(TariffDao.class);
    GroupService groupService = mock(GroupService.class);
    SocketDao socketDao = mock(SocketDao.class);

    TariffResolver resolver = new TariffResolver(contracts, applicabilities, campsiteDefaults, tariffDao, groupService,
            socketDao);

    Socket socket;
    Group campsite;

    @BeforeEach
    void setUp() {
        campsite = new Group();
        campsite.setId(500L);
        Pedestal pedestal = new Pedestal();
        pedestal.setId(40L);
        pedestal.setCode("HR-BAY-P001");
        pedestal.setGroup(campsite);
        socket = new Socket();
        socket.setId(41L);
        pedestal.addSocket(socket);
    }

    static Tariff tariff(String name) {
        return new Tariff(name, name, Market.HR);
    }

    @Test
    void seasonalContractTariffOverridesTheSocketTariff() {
        Tariff contract = tariff("SEASON CONTRACT");
        when(contracts.findContractTariff(socket, 7L, AT)).thenReturn(contract);

        ResolvedTariff resolved = resolver.resolve(socket, 7L, AT);

        assertThat(resolved.tariff()).isEqualTo(contract);
        assertThat(resolved.source()).isEqualTo(TariffSource.SEASONAL_CONTRACT);
        verifyNoInteractions(applicabilities);
    }

    @Test
    void socketTariffComesBeforeThePedestalTariff() {
        Tariff socketTariff = tariff("SOCKET");
        TariffApplicability assignment = TariffApplicability.forSocket(socket, socketTariff, AT.minusDays(1));
        when(applicabilities.findActiveTariffForSocket(41L, AT)).thenReturn(assignment);

        assertThat(resolver.resolve(socket, null, AT).source()).isEqualTo(TariffSource.SOCKET);
    }

    @Test
    void pedestalTariffComesBeforeTheCampsiteDefault() {
        Tariff pedestalTariff = tariff("PEDESTAL");
        when(applicabilities.findActiveTariffForPedestal(40L, AT))
                .thenReturn(TariffApplicability.forPedestal(socket.getPedestal(), pedestalTariff, AT));
        when(campsiteDefaults.findDefaultTariff(500L)).thenReturn(tariff("CAMPSITE"));

        assertThat(resolver.resolve(socket, null, AT)).isEqualTo(new ResolvedTariff(pedestalTariff,
                TariffSource.PEDESTAL));
    }

    @Test
    void campsiteDefaultComesBeforeTheMarketDefault() {
        Tariff campsiteTariff = tariff("CAMPSITE");
        when(campsiteDefaults.findDefaultTariff(500L)).thenReturn(campsiteTariff);
        when(groupService.determineMarketByGroupId(500L)).thenReturn(Market.HR);
        when(tariffDao.findDefaultTariffForMarket(Market.HR)).thenReturn(tariff("HR STANDARD"));

        assertThat(resolver.resolve(socket, null, AT).tariff()).isEqualTo(campsiteTariff);
    }

    @Test
    void marketDefaultIsTheLastStep() {
        Tariff standard = tariff("HR STANDARD");
        when(groupService.determineMarketByGroupId(500L)).thenReturn(Market.HR);
        when(tariffDao.findDefaultTariffForMarket(Market.HR)).thenReturn(standard);

        assertThat(resolver.resolve(socket, null, AT)).isEqualTo(new ResolvedTariff(standard,
                TariffSource.MARKET_DEFAULT));
    }

    @Test
    void groupWithoutMarketHasNoTariff() {
        when(groupService.determineMarketByGroupId(500L)).thenReturn(null);

        assertThatThrownBy(() -> resolver.resolve(socket, null, AT)).isInstanceOf(NoTariffFoundException.class)
                .hasMessageContaining("market null");
    }

    @Test
    void marketWithoutDefaultTariffHasNoTariff() {
        when(groupService.determineMarketByGroupId(500L)).thenReturn(Market.FR);

        assertThatThrownBy(() -> resolver.resolve(socket, null, AT)).isInstanceOf(NoTariffFoundException.class);
    }

    @Test
    void pedestalOutsideAnyGroupHasNoTariffBeyondItsAssignments() {
        socket.getPedestal().setGroup(null);

        assertThatThrownBy(() -> resolver.resolve(socket, null, AT)).isInstanceOf(NoTariffFoundException.class)
                .hasMessageContaining("hangs in no group");
        verifyNoInteractions(campsiteDefaults);
    }

    @Test
    void unknownSocketIsRefused() {
        when(socketDao.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolve(99L, null, AT)).isInstanceOf(IllegalArgumentException.class);
    }
}
