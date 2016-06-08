package io.pillopl.power;

import io.pillopl.card.Authorization;
import io.pillopl.card.CardFixture;
import io.pillopl.card.ValidationStatus;
import io.pillopl.event.Event;
import io.pillopl.event.EventService;
import io.pillopl.event.EventType;
import io.pillopl.event.PowerSectionMisconfiguredEvent;
import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Socket;
import io.pillopl.persistence.dao.PowerSectionDao;
import io.pillopl.persistence.dao.SocketDao;
import io.pillopl.supply.StopReason;
import io.pillopl.supply.SupplyControlUnitImpl;
import io.pillopl.supply.SupplyRequest;
import io.pillopl.test.IntegrationTest;
import io.pillopl.value.SocketLinkId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Section power budget on the database, asked through the card authorization: 16 A sockets on a 32 A section.
 */
@Transactional
class PowerSectionIntegrationTest extends IntegrationTest {

    @Autowired Authorization authorization;
    @Autowired SupplyControlUnitImpl controlUnit;
    @Autowired PowerSectionService powerSectionService;
    @Autowired PowerSectionDao powerSectionDao;
    @Autowired SocketDao socketDao;
    @Autowired EventService eventService;
    @Autowired GroupService groupService;
    @Autowired GroupManager groupManager;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired CardFixture cards;
    @Autowired JdbcTemplate jdbcTemplate;

    final LocalDateTime plugIn = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).minusHours(1);
    PowerSection meadow;

    @BeforeEach
    void setUp() {
        Group bay = groupManager.createChildGroup(groupService.fetchSiteGroup("HR SITES"), "BAY POWER", "Bay");
        Group club = groupManager.createChildGroup(groupService.fetchIssuerGroup("HR ISSUERS"), "POWER CLUB",
                "Club");
        groupManager.addAcceptanceAgreement(club, bay);
        infrastructure.pedestal("HR-BAY-P201", bay, 4);
        for (String rfid : new String[] {"8201", "8202", "8203"}) {
            cards.card(rfid, "HR-" + rfid, cards.guest("Guest " + rfid, club), LocalDate.now().minusDays(3),
                    LocalDate.now().plusDays(3));
        }
        meadow = powerSectionDao.saveAndFlush(new PowerSection("Meadow", new BigDecimal("32"),
                NominalCurrentSumAlgorithm.BEAN_NAME));
        for (int socketNo = 1; socketNo <= 3; socketNo++) {
            Socket socket = socket(socketNo);
            socket.setPowerSection(meadow);
            socketDao.saveAndFlush(socket);
        }
    }

    @Test
    void theAuthorizationIsRefusedAsUnavailableWhenTheSectionIsFullyAllocated() {
        assertThat(authorize("8202", 2)).isEqualTo(ValidationStatus.ACCEPTED);
        start("8201", 1);
        assertThat(authorize("8202", 2)).isEqualTo(ValidationStatus.ACCEPTED);
        start("8202", 2);

        assertThat(authorize("8203", 3)).isEqualTo(ValidationStatus.UNAVAILABLE);
    }

    @Test
    void theGuestWhoseSessionRunsOnTheSocketIsNotCountedTwice() {
        start("8201", 1);
        start("8202", 2);

        assertThat(authorize("8201", 1)).isEqualTo(ValidationStatus.ACCEPTED);
    }

    @Test
    void aStoppedSessionGivesItsCurrentBackToTheSection() {
        start("8201", 1);
        Integer transactionId = start("8202", 2);
        controlUnit.stopSupply("HR-BAY-P201", "8202", SupplyRequest.stop(transactionId, new BigDecimal("12"),
                plugIn.plusMinutes(30), StopReason.UNPLUGGED));

        assertThat(authorize("8203", 3)).isEqualTo(ValidationStatus.ACCEPTED);
    }

    @Test
    void aSocketWithoutSectionHasNoLimit() {
        start("8201", 1);
        start("8202", 2);

        assertThat(authorize("8203", 4)).isEqualTo(ValidationStatus.ACCEPTED);
    }

    @Test
    void aSectionWithAnUnknownAlgorithmRefusesEverySupplyAndRaisesAnAlert() {
        meadow.setAlgorithmBeanName("forecastAlgorithm");
        powerSectionDao.saveAndFlush(meadow);
        eventService.registerQueue("test-power-section", EventType.POWER_SECTION_MISCONFIGURED);
        try {
            assertThat(authorize("8201", 1)).isEqualTo(ValidationStatus.UNAVAILABLE);

            Event alert = eventService.subscribeToQueue("test-power-section").poll();
            assertThat(PowerSectionMisconfiguredEvent.getPowerSectionId(alert)).isEqualTo(meadow.getId());
            assertThat(PowerSectionMisconfiguredEvent.getProblem(alert))
                    .isEqualTo(PowerSectionService.UNKNOWN_ALGORITHM);
        } finally {
            eventService.unregisterQueue("test-power-section", EventType.POWER_SECTION_MISCONFIGURED);
        }
    }

    @Test
    void aBeanThatIsNoSectionAlgorithmRefuses() {
        meadow.setAlgorithmBeanName("powerSectionService");
        powerSectionDao.saveAndFlush(meadow);

        assertThat(powerSectionService.isWithinSectionBudget(SocketLinkId.of("HR-BAY-P201", 1))).isFalse();
    }

    @Test
    void aSectionNeedsAPositiveMaximumCurrent() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO POWER_SECTION (SECTION_NAME, MAX_CURRENT_A) VALUES (?, ?)", "Dunes", 0))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void sectionNamesAreUnique() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO POWER_SECTION (SECTION_NAME, MAX_CURRENT_A) VALUES (?, ?)", "Meadow", 16))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private ValidationStatus authorize(String rfid, int socketNo) {
        return authorization.authenticate(rfid, SocketLinkId.of("HR-BAY-P201", socketNo)).getStatus();
    }

    private Integer start(String rfid, int socketNo) {
        return controlUnit.startSupply(SocketLinkId.of("HR-BAY-P201", socketNo), rfid,
                SupplyRequest.start(new BigDecimal("10"), plugIn)).getTransactionId();
    }

    private Socket socket(int socketNo) {
        return socketDao.findByPedestalCodeAndSocketNo("HR-BAY-P201", socketNo);
    }
}
