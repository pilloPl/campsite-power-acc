package io.pillopl.supply;

import io.pillopl.card.Authorization;
import io.pillopl.card.CardFixture;
import io.pillopl.card.ValidationStatus;
import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.PedestalService;
import io.pillopl.infrastructure.PedestalState;
import io.pillopl.infrastructure.Socket;
import io.pillopl.persistence.dao.PedestalStateDao;
import io.pillopl.persistence.dao.SocketDao;
import io.pillopl.persistence.dao.SupplyIncidentDao;
import io.pillopl.persistence.dao.SupplySessionDao;
import io.pillopl.supply.SupplyIncident.IncidentType;
import io.pillopl.test.IntegrationTest;
import io.pillopl.util.DateService;
import io.pillopl.value.SocketLinkId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Supply sessions on the database: start, repeated start, stop, incidents, occupied sockets and stale sessions
 * (stale = socket and pedestal silent for the timeout, not merely started long ago).
 */
@Transactional
class SupplySessionIntegrationTest extends IntegrationTest {

    @Autowired SupplyControlUnitImpl controlUnit;
    @Autowired SupplySessionManager sessionManager;
    @Autowired SupplySessionDao supplySessionDao;
    @Autowired SupplyIncidentDao supplyIncidentDao;
    @Autowired SocketDao socketDao;
    @Autowired Authorization authorization;
    @Autowired GroupService groupService;
    @Autowired GroupManager groupManager;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired CardFixture cards;
    @Autowired DateService dateService;
    @Autowired PedestalService pedestalService;
    @Autowired PedestalStateDao pedestalStateDao;

    final SocketLinkId socket1 = SocketLinkId.of("HR-BAY-P101", 1);
    final LocalDateTime plugIn = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).minusHours(2);
    Pedestal pedestal;

    @BeforeEach
    void setUp() {
        Group bay = groupManager.createChildGroup(groupService.fetchSiteGroup("HR SITES"), "BAY SUPPLY", "Bay");
        Group club = groupManager.createChildGroup(groupService.fetchIssuerGroup("HR ISSUERS"), "SUPPLY CLUB",
                "Club");
        groupManager.addAcceptanceAgreement(club, bay);
        pedestal = infrastructure.pedestal("HR-BAY-P101", bay, 2);
        pedestal.setBillable(true);
        cards.card("8101", "HR-8101", cards.guest("Kovac", club), LocalDate.now().minusDays(3),
                LocalDate.now().plusDays(3));
        cards.card("8102", "HR-8102", cards.guest("Novak", club), LocalDate.now().minusDays(3),
                LocalDate.now().plusDays(3));
    }

    @AfterEach
    void resetClock() {
        dateService.setClock(null);
    }

    @Test
    void acceptedStartOpensASessionWithATransactionIdFromTheSequence() {
        SupplyResponse response = controlUnit.startSupply(socket1, "8101",
                SupplyRequest.start(new BigDecimal("100.500"), plugIn));

        assertThat(response.getStatus()).isEqualTo(ValidationStatus.ACCEPTED);
        assertThat(response.getTransactionId()).isGreaterThanOrEqualTo(1000);
        assertThat(response.getMaxCurrentA()).isEqualTo(16);
        assertThat(response.getGreeting()).isEqualTo("Kovac");
        assertThat(response.getPriceInformation()).contains("0.45");
        SupplySession session = sessionManager.findSessionFor("HR-BAY-P101", response.getTransactionId());
        assertThat(session.isActive()).isTrue();
        assertThat(session.getCardRfid()).isEqualTo("8101");
        assertThat(session.getRemoteDate()).isEqualTo(plugIn);
    }

    @Test
    void repeatedStartOfThePedestalIsAnsweredWithTheSameSession() {
        SupplyResponse first = controlUnit.startSupply(socket1, "8101", SupplyRequest.start(new BigDecimal("100.5"),
                plugIn));
        SupplyResponse repeated = controlUnit.startSupply(socket1, "8101",
                SupplyRequest.start(new BigDecimal("100.500"), plugIn));

        assertThat(repeated.getTransactionId()).isEqualTo(first.getTransactionId());
        assertThat(supplySessionDao.findActiveBySocket(socket("HR-BAY-P101", 1))).hasSize(1);
    }

    @Test
    void socketSupplyingForOneGuestIsOccupiedForAnother() {
        controlUnit.startSupply(socket1, "8101", SupplyRequest.start(new BigDecimal("100.5"), plugIn));

        assertThat(authorization.authenticate("8102", socket1).getStatus()).isEqualTo(ValidationStatus.SOCKET_OCCUPIED);
        assertThat(authorization.authenticate("8101", socket1).getStatus()).isEqualTo(ValidationStatus.ACCEPTED);
        assertThat(authorization.authenticate("8102", SocketLinkId.of("HR-BAY-P101", 2)).getStatus())
                .isEqualTo(ValidationStatus.ACCEPTED);
    }

    @Test
    void stopClosesTheSessionAndStoresTheStopReadingOnTheSocket() {
        Integer transactionId = controlUnit.startSupply(socket1, "8101",
                SupplyRequest.start(new BigDecimal("100.5"), plugIn)).getTransactionId();

        SupplyResponse response = controlUnit.stopSupply("HR-BAY-P101", "8101", SupplyRequest.stop(transactionId,
                new BigDecimal("112.25"), plugIn.plusHours(1), StopReason.UNPLUGGED));

        assertThat(response.getStatus()).isEqualTo(ValidationStatus.ACCEPTED);
        SupplySession session = sessionManager.findSessionFor("HR-BAY-P101", transactionId);
        assertThat(session.isActive()).isFalse();
        assertThat(session.getStopReason()).isEqualTo(StopReason.UNPLUGGED);
        assertThat(session.getStopDate()).isEqualTo(plugIn.plusHours(1));
        assertThat(socket("HR-BAY-P101", 1).getMeterKwh()).isEqualByComparingTo("112.25");
        assertThat(authorization.authenticate("8102", socket1).getStatus()).isEqualTo(ValidationStatus.ACCEPTED);
    }

    @Test
    void stopOfAnUnknownTransactionIsAcceptedAndKeptInTheIncidentLog() {
        SupplyResponse response = controlUnit.stopSupply("HR-BAY-P101", "8101", SupplyRequest.stop(999999,
                new BigDecimal("112.25"), plugIn.plusHours(1), StopReason.LOCAL));

        assertThat(response.getStatus()).isEqualTo(ValidationStatus.ACCEPTED);
        assertThat(supplyIncidentDao.findByPedestalCodeOrderByCreateDateDesc("HR-BAY-P101"))
                .extracting(SupplyIncident::getIncidentType, SupplyIncident::getTransactionId)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(IncidentType.NO_SESSION, 999999));
    }

    @Test
    void sessionWithoutStopIsClosedAfterTheTimeoutWithTheLastSocketReading() {
        LocalDateTime longAgo = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).minusHours(30);
        Integer transactionId = controlUnit.startSupply(socket1, "8101",
                SupplyRequest.start(new BigDecimal("100.5"), longAgo)).getTransactionId();
        Integer recent = controlUnit.startSupply(SocketLinkId.of("HR-BAY-P101", 2), "8102",
                SupplyRequest.start(new BigDecimal("7"), plugIn)).getTransactionId();
        Socket socket = socket("HR-BAY-P101", 1);
        socket.setMeterKwh(new BigDecimal("131.75"));
        socket.setModDate(longAgo.plusHours(5));
        socketDao.saveAndFlush(socket);

        int closed = controlUnit.closeStaleSessions();

        assertThat(closed).isEqualTo(1);
        SupplySession session = sessionManager.findSessionFor("HR-BAY-P101", transactionId);
        assertThat(session.isActive()).isFalse();
        assertThat(session.getStopReason()).isEqualTo(StopReason.TIMEOUT);
        assertThat(session.getStopDate()).isEqualTo(longAgo.plusHours(5));
        assertThat(sessionManager.findSessionFor("HR-BAY-P101", recent).isActive()).isTrue();
    }

    @Test
    void lateStopOfASessionClosedByTheTimeoutIsAnIncident() {
        LocalDateTime longAgo = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).minusHours(30);
        Integer transactionId = controlUnit.startSupply(socket1, "8101",
                SupplyRequest.start(new BigDecimal("100.5"), longAgo)).getTransactionId();
        silentSince(socket("HR-BAY-P101", 1), longAgo);
        controlUnit.closeStaleSessions();

        SupplyResponse response = controlUnit.stopSupply("HR-BAY-P101", "8101", SupplyRequest.stop(transactionId,
                new BigDecimal("140"), LocalDateTime.now(), StopReason.LOCAL));

        assertThat(response.getStatus()).isEqualTo(ValidationStatus.ACCEPTED);
        assertThat(supplyIncidentDao.findByPedestalCodeOrderByCreateDateDesc("HR-BAY-P101"))
                .extracting(SupplyIncident::getIncidentType).containsExactly(IncidentType.SESSION_ALREADY_CLOSED);
    }

    @Test
    void multiNightSupplyThatKeepsSendingMeterReadingsIsNotClosedByTheTimeout() {
        LocalDateTime threeDaysAgo = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).minusDays(3);
        Integer transactionId = controlUnit.startSupply(socket1, "8101",
                SupplyRequest.start(new BigDecimal("100"), threeDaysAgo)).getTransactionId();
        Socket socket = socket("HR-BAY-P101", 1);
        for (int hour = 1; hour < 72; hour++) {
            pedestalService.updateMeterReading(socket, new BigDecimal(100 + hour), threeDaysAgo.plusHours(hour));
        }
        socketDao.flush();

        assertThat(controlUnit.closeStaleSessions()).isZero();
        assertThat(sessionManager.findSessionFor("HR-BAY-P101", transactionId).isActive()).isTrue();
    }

    @Test
    void sessionWithoutStayOnASilentSocketIsKeptOpenWhileThePedestalStillSendsMessages() {
        LocalDateTime twoDaysAgo = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).minusDays(2);
        Integer transactionId = controlUnit.startSupply(socket1, "8101",
                SupplyRequest.start(new BigDecimal("100"), twoDaysAgo)).getTransactionId();
        silentSince(socket("HR-BAY-P101", 1), twoDaysAgo);
        pedestalStateDao.saveAndFlush(new PedestalState(pedestal, LocalDateTime.now().minusMinutes(10)));

        assertThat(controlUnit.closeStaleSessions()).isZero();
        SupplySession session = sessionManager.findSessionFor("HR-BAY-P101", transactionId);
        assertThat(session.isActive()).isTrue();
        assertThat(session.getStayId()).isNull();
    }

    @Test
    void sessionOfASocketAndPedestalSilentForTheTimeoutIsClosed() {
        LocalDateTime twoDaysAgo = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).minusDays(2);
        Integer transactionId = controlUnit.startSupply(socket1, "8101",
                SupplyRequest.start(new BigDecimal("100"), twoDaysAgo)).getTransactionId();
        silentSince(socket("HR-BAY-P101", 1), twoDaysAgo.plusHours(20));
        pedestalStateDao.saveAndFlush(new PedestalState(pedestal, twoDaysAgo.plusHours(20)));

        assertThat(controlUnit.closeStaleSessions()).isEqualTo(1);
        assertThat(sessionManager.findSessionFor("HR-BAY-P101", transactionId).getStopReason())
                .isEqualTo(StopReason.TIMEOUT);
    }

    private void silentSince(Socket socket, LocalDateTime lastSignOfLife) {
        socket.setModDate(lastSignOfLife);
        socketDao.saveAndFlush(socket);
    }

    private Socket socket(String pedestalCode, int socketNo) {
        return socketDao.findByPedestalCodeAndSocketNo(pedestalCode, socketNo);
    }
}
