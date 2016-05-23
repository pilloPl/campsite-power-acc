package io.pillopl.pricing;

import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.group.Market;
import io.pillopl.group.attribute.DefaultTariffService;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.Socket;
import io.pillopl.persistence.dao.UserinfoDao;
import io.pillopl.test.IntegrationTest;
import io.pillopl.user.Userinfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class TariffApplicabilityServiceIntegrationTest extends IntegrationTest {

    static final LocalDateTime JULY = LocalDateTime.of(2026, 7, 1, 0, 0);

    @Autowired TariffApplicabilityService tariffApplicabilityService;
    @Autowired TariffResolver tariffResolver;
    @Autowired DefaultTariffService defaultTariffService;
    @Autowired PricingFixture pricing;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired GroupManager groupManager;
    @Autowired GroupService groupService;
    @Autowired UserinfoDao userinfoDao;

    Group bay;
    Pedestal first, second;
    Tariff bayTariff;

    @BeforeEach
    void setUp() {
        bay = groupManager.createChildGroup(groupService.fetchSiteGroup("HR SITES"), "BAY CAMPSITE", "Bay");
        first = infrastructure.pedestal("HR-BAY-P001", bay, 2);
        second = infrastructure.pedestal("HR-BAY-P002", bay, 1);
        bayTariff = pricing.energyTariff("BAY", Market.HR, "0.42");
    }

    @Test
    void affiliateOwnersPedestalsAllGetTheTariff() {
        Userinfo owner = new Userinfo("bay.owner", "Bay owner", "owner@example.org");
        owner.setGroup(groupService.fetchStaffGroup("HR STAFF"));
        owner.getGroups().add(bay);
        owner = userinfoDao.saveAndFlush(owner);

        List<TariffApplicability> applied = tariffApplicabilityService.applyTariffToAffiliatePedestals(owner,
                bayTariff.getId(), JULY);

        assertThat(applied).hasSize(2).allMatch(a -> "bay.owner".equals(a.getCreatedBy()));
        Socket socket = second.getSockets().get(0);
        assertThat(tariffResolver.resolve(socket, null, JULY).source()).isEqualTo(TariffSource.PEDESTAL);
    }

    @Test
    void socketGetsItsOwnTariffAndLosesThePlannedOne() {
        Socket socket = first.getSockets().get(0);
        tariffApplicabilityService.applyTariffToSocket(socket.getId(), bayTariff.getId(), JULY, "manager");
        assertThat(tariffApplicabilityService.findAppliedTariff(socket.getId(), JULY).getTariff()).isEqualTo(bayTariff);

        assertThat(tariffApplicabilityService.applyTariffToSocket(socket.getId(), null, JULY, "manager")).isNull();
        assertThat(tariffApplicabilityService.findAppliedTariff(socket.getId(), JULY)).isNull();
    }

    @Test
    void assignmentWithoutStartDateIsRefused() {
        assertThatThrownBy(() -> tariffApplicabilityService.applyTariffToPedestal(first.getId(), bayTariff.getId(),
                null, "manager")).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("first day");
    }

    @Test
    void tariffOfAnotherMarketIsNotAssigned() {
        Tariff italian = pricing.energyTariff("LAGO", Market.IT, "0.55");

        assertThatThrownBy(() -> tariffApplicabilityService.applyTariffToPedestal(first.getId(), italian.getId(),
                JULY, "manager")).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("market IT");
    }

    @Test
    void unknownPedestalOrSocketIsSkipped() {
        assertThat(tariffApplicabilityService.applyTariffToPedestal(999_999L, bayTariff.getId(), JULY, "x")).isNull();
        assertThat(tariffApplicabilityService.applyTariffToSocket(999_999L, bayTariff.getId(), JULY, "x")).isNull();
    }

    @Test
    void campsiteDefaultIsSetAndRemoved() {
        tariffApplicabilityService.setCampsiteDefaultTariff(bay, bayTariff.getId(), "manager");
        assertThat(defaultTariffService.findDefaultTariff(bay.getId())).isEqualTo(bayTariff);

        tariffApplicabilityService.setCampsiteDefaultTariff(bay, null, "manager");
        assertThat(defaultTariffService.findDefaultTariff(bay.getId())).isNull();
    }

    @Test
    void historyOfASocketHoldsItsOwnAndItsPedestalsAssignments() {
        Socket socket = first.getSockets().get(1);
        tariffApplicabilityService.applyTariffToPedestal(first.getId(), bayTariff.getId(), JULY, "manager");
        tariffApplicabilityService.applyTariffToSocket(socket.getId(), bayTariff.getId(), JULY.plusDays(5), "manager");

        assertThat(tariffApplicabilityService.findAssignmentHistory(socket)).extracting(TariffApplicability::isForSocket)
                .containsExactly(true, false);
    }
}
