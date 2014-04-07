package io.pillopl.pricing;

import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.group.Market;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.Socket;
import io.pillopl.persistence.dao.TariffApplicabilityDao;
import io.pillopl.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class TariffApplicabilityDaoIntegrationTest extends IntegrationTest {

    static final LocalDateTime JUNE = LocalDateTime.of(2026, 6, 1, 0, 0);
    static final LocalDateTime JULY = LocalDateTime.of(2026, 7, 1, 0, 0);

    @Autowired TariffApplicabilityDao tariffApplicabilityDao;
    @Autowired PricingFixture pricing;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired GroupManager groupManager;
    @Autowired GroupService groupService;

    Pedestal pedestal;
    Socket socket;
    Tariff spring, summer;

    @BeforeEach
    void setUp() {
        Group bay = groupManager.createChildGroup(groupService.fetchSiteGroup("HR SITES"), "BAY CAMPSITE", "Bay");
        pedestal = infrastructure.pedestal("HR-BAY-P001", bay, 2);
        socket = pedestal.getSockets().get(0);
        spring = pricing.energyTariff("SPRING", Market.HR, "0.40");
        summer = pricing.energyTariff("SUMMER", Market.HR, "0.60");
    }

    @Test
    void newestAssignmentStartedBeforeThePlugInIsActive() {
        tariffApplicabilityDao.saveAndFlush(TariffApplicability.forSocket(socket, spring, JUNE));
        tariffApplicabilityDao.saveAndFlush(TariffApplicability.forSocket(socket, summer, JULY));

        assertThat(tariffApplicabilityDao.findActiveTariffForSocket(socket.getId(), JUNE.plusDays(3)).getTariff())
                .isEqualTo(spring);
        assertThat(tariffApplicabilityDao.findActiveTariffForSocket(socket.getId(), JULY).getTariff())
                .isEqualTo(summer);
        assertThat(tariffApplicabilityDao.findActiveTariffForSocket(socket.getId(), JUNE.minusSeconds(1))).isNull();
    }

    @Test
    void pedestalAssignmentIsFoundSeparatelyFromSocketAssignments() {
        tariffApplicabilityDao.saveAndFlush(TariffApplicability.forPedestal(pedestal, summer, JUNE));

        assertThat(tariffApplicabilityDao.findActiveTariffForPedestal(pedestal.getId(), JULY).getTariff())
                .isEqualTo(summer);
        assertThat(tariffApplicabilityDao.findActiveTariffForSocket(socket.getId(), JULY)).isNull();
    }

    @Test
    void plannedAssignmentsAreRemovedAndHistoryStays() {
        tariffApplicabilityDao.saveAndFlush(TariffApplicability.forSocket(socket, spring, JUNE));
        tariffApplicabilityDao.saveAndFlush(TariffApplicability.forSocket(socket, summer, JULY));

        assertThat(tariffApplicabilityDao.deleteBySocketIdStartingFrom(socket.getId(), JUNE.plusDays(10))).isEqualTo(1);
        assertThat(tariffApplicabilityDao.findBySocketId(socket.getId())).extracting(TariffApplicability::getTariff)
                .containsExactly(spring);
    }

    @Test
    void assignmentTargetsEitherASocketOrAPedestal() {
        TariffApplicability both = TariffApplicability.forSocket(socket, spring, JUNE);
        both.setPedestal(pedestal);

        assertThatThrownBy(() -> tariffApplicabilityDao.saveAndFlush(both))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
