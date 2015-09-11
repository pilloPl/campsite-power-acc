package io.pillopl.qrpay;

import io.pillopl.api.exception.AccessNotAllowedException;
import io.pillopl.api.exception.ConflictException;
import io.pillopl.api.exception.ObjectNotFoundException;
import io.pillopl.card.CardFixture;
import io.pillopl.card.ValidationStatus;
import io.pillopl.consumption.ConsumptionRecord;
import io.pillopl.consumption.PaymentChannel;
import io.pillopl.dto.qrpay.QrPayProcessDTO;
import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.PedestalService;
import io.pillopl.infrastructure.Socket;
import io.pillopl.pedestallink.v1.PedestalLinkClientV1;
import io.pillopl.persistence.dao.ConsumptionRecordDao;
import io.pillopl.persistence.dao.QrPayProcessDao;
import io.pillopl.persistence.dao.SocketDao;
import io.pillopl.persistence.dao.UserinfoDao;
import io.pillopl.persistence.dao.UserroleDao;
import io.pillopl.qrpay.state.QrPayStateFactory;
import io.pillopl.supply.StopReason;
import io.pillopl.supply.SupplyControlUnitImpl;
import io.pillopl.supply.SupplyRequest;
import io.pillopl.supply.SupplyResponse;
import io.pillopl.supply.SupplySession;
import io.pillopl.supply.SupplySessionManager;
import io.pillopl.test.IntegrationTest;
import io.pillopl.user.Userinfo;
import io.pillopl.user.UserinfoStatus;
import io.pillopl.user.UserroleName;
import io.pillopl.util.DateService;
import io.pillopl.value.SocketLinkId;
import io.pillopl.value.Username;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * QR payment on the database: the row chain from the app's start through the pedestal's supply to the price of
 * the consumption record, the owner check, refused starts and the archive. The pedestal link is a mock; the
 * pedestal's messages go through the supply control unit, its events are signalled to the supervisor directly.
 */
@Transactional
class QrPayProcessIntegrationTest extends IntegrationTest {

    static final String SOCKET_CODE = "HRBAY*P201*1";
    static final Username APP = Username.of("payment-app");

    @MockitoBean PedestalLinkClientV1 pedestalLink;

    @Autowired QrPayProcessSupervisor supervisor;
    @Autowired QrPayProcessService qrPayProcessService;
    @Autowired QrPayStateFactory stateFactory;
    @Autowired QrPayProcessDao qrPayProcessDao;
    @Autowired SupplyControlUnitImpl controlUnit;
    @Autowired SupplySessionManager sessionManager;
    @Autowired ConsumptionRecordDao consumptionRecordDao;
    @Autowired PedestalService pedestalService;
    @Autowired SocketDao socketDao;
    @Autowired UserinfoDao userinfoDao;
    @Autowired UserroleDao userroleDao;
    @Autowired GroupService groupService;
    @Autowired GroupManager groupManager;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired CardFixture cards;
    @Autowired DateService dateService;
    @Autowired JdbcTemplate jdbcTemplate;

    final SocketLinkId socket1 = SocketLinkId.of("HR-BAY-P201", 1);
    final LocalDateTime plugIn = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).minusHours(2);
    Pedestal pedestal;

    @BeforeEach
    void setUp() {
        Group bay = groupManager.createChildGroup(groupService.fetchSiteGroup("HR SITES"), "BAY QR", "Bay");
        pedestal = infrastructure.pedestal("HR-BAY-P201", bay, 2);
        pedestal.setBillable(true);
        Socket socket = socket(1);
        socket.setSocketCode(SOCKET_CODE);
        socketDao.saveAndFlush(socket);
        appClient("payment-app");
        appClient("other-app");
        supervisor.setSignalRetryPauseInMillis(0);
        when(pedestalLink.remoteSupplyStart(any(), anyInt(), anyString())).thenReturn(true);
        when(pedestalLink.remoteSupplyStop(any(), anyInt())).thenReturn(true);
    }

    @AfterEach
    void resetClock() {
        dateService.setClock(null);
    }

    @Test
    void paymentRunsFromTheAppsStartToThePriceOfTheConsumptionRecord() {
        QrPayProcessDTO started = supervisor.startQrPay(APP, "ORD-1", SOCKET_CODE, "guest-7");

        assertThat(started.state()).isEqualTo("SOCKET_START_WAITING");
        assertThat(started.priceInformation()).contains("0.45");
        String authTag = "QR" + started.processId();
        verify(pedestalLink).remoteSupplyStart(any(Pedestal.class), eq(1), eq(authTag));

        SupplyResponse startResponse = controlUnit.startSupply(socket1, authTag,
                SupplyRequest.start(new BigDecimal("100.5"), plugIn));
        assertThat(startResponse.getStatus()).isEqualTo(ValidationStatus.ACCEPTED);
        SupplySession session = sessionManager.findSessionFor("HR-BAY-P201", startResponse.getTransactionId());
        assertThat(session.getQrPayProcessId()).isEqualTo(started.processId());
        assertThat(session.getCard()).isNull();

        supervisor.onSupplyStarted(started.processId(), session.getId(), session.getTransactionId());
        pedestalService.updateMeterReading(socket(1), new BigDecimal("110.5"), plugIn.plusMinutes(30));
        QrPayProcessDTO running = supervisor.getQrPayProcess(APP, "ORD-1", "guest-7");
        assertThat(running.state()).isEqualTo("START_CONFIRMED");
        assertThat(running.supplyStart()).isEqualTo(plugIn);
        assertThat(running.kwh()).isEqualByComparingTo("10");
        assertThat(running.preliminaryCost()).isEqualByComparingTo("4.50");
        assertThat(running.cost()).isNull();

        QrPayProcessDTO stopping = supervisor.stopQrPay(APP, "ORD-1", "guest-7");
        assertThat(stopping.state()).isEqualTo("SOCKET_STOP_WAITING");
        verify(pedestalLink).remoteSupplyStop(any(Pedestal.class), eq(session.getTransactionId()));

        controlUnit.stopSupply("HR-BAY-P201", authTag, SupplyRequest.stop(session.getTransactionId(),
                new BigDecimal("112.5"), plugIn.plusHours(1), StopReason.REMOTE));
        supervisor.onSupplyStopped(started.processId(), session.getId(), session.getTransactionId());

        ConsumptionRecord record = consumptionRecordDao.findBySupplySessionId(session.getId());
        assertThat(record.getPaymentChannel()).isEqualTo(PaymentChannel.QR_APP);
        QrPayProcessDTO closed = supervisor.getQrPayProcess(APP, "ORD-1", "guest-7");
        assertThat(closed.state()).isEqualTo("CLOSED");
        assertThat(closed.cost()).isEqualByComparingTo(record.getCost()).isEqualByComparingTo("5.40");
        assertThat(closed.kwh()).isEqualByComparingTo("12");
        assertThat(closed.preliminaryCost()).isNull();
        assertThat(qrPayProcessDao.findByProcessIdOrderById(started.processId()))
                .extracting(QrPayProcess::getState)
                .containsExactly("START_REQUESTED", "SOCKET_START_WAITING", "SOCKET_START_RECEIVED",
                        "START_CONFIRMED", "STOP_REQUESTED", "SOCKET_STOP_WAITING", "SOCKET_STOP_RECEIVED",
                        "CALCULATION_REQUESTED", "CALCULATION_DONE", "CLOSED");
    }

    @Test
    void everyRowButTheCurrentOneIsLinkedToItsSuccessor() {
        Long processId = supervisor.startQrPay(APP, "ORD-2", SOCKET_CODE, "guest-7").processId();

        var chain = qrPayProcessDao.findByProcessIdOrderById(processId);

        assertThat(chain).hasSize(2);
        assertThat(chain.get(0).getNextProcessId()).isEqualTo(chain.get(1).getId());
        assertThat(chain.get(0).getStopTime()).isNotNull();
        assertThat(chain.get(1).getPreviousProcessId()).isEqualTo(chain.get(0).getId());
        assertThat(chain.get(1).isCurrent()).isTrue();
        assertThat(qrPayProcessService.findCurrent(processId).getId()).isEqualTo(chain.get(1).getId());
    }

    @Test
    void guestUnpluggingEndsThePaymentWithoutTheAppsStop() {
        QrPayProcessDTO started = supervisor.startQrPay(APP, "ORD-3", SOCKET_CODE, "guest-7");
        SupplySession session = startSupply(started);
        supervisor.onSupplyStarted(started.processId(), session.getId(), session.getTransactionId());

        controlUnit.stopSupply("HR-BAY-P201", null, SupplyRequest.stop(session.getTransactionId(),
                new BigDecimal("101.5"), plugIn.plusHours(1), StopReason.UNPLUGGED));
        supervisor.onSupplyStopped(started.processId(), session.getId(), session.getTransactionId());

        QrPayProcessDTO closed = supervisor.getQrPayProcess(APP, "ORD-3", "guest-7");
        assertThat(closed.state()).isEqualTo("CLOSED");
        assertThat(closed.cost()).isEqualByComparingTo("0.45");
        verify(pedestalLink, never()).remoteSupplyStop(any(), anyInt());
    }

    @Test
    void onlyTheClientAndTheAppUserThatStartedThePaymentMayStopIt() {
        supervisor.startQrPay(APP, "ORD-4", SOCKET_CODE, "guest-7");

        assertThatThrownBy(() -> supervisor.stopQrPay(APP, "ORD-4", "guest-8"))
                .isInstanceOf(AccessNotAllowedException.class);
        assertThatThrownBy(() -> supervisor.stopQrPay(Username.of("other-app"), "ORD-4", "guest-7"))
                .isInstanceOf(AccessNotAllowedException.class);
        assertThatThrownBy(() -> supervisor.getQrPayProcess(APP, "ORD-4", "guest-8"))
                .isInstanceOf(AccessNotAllowedException.class);
        assertThat(qrPayProcessService.findCurrentByOrderReference("ORD-4").getState())
                .isEqualTo("SOCKET_START_WAITING");
        verify(pedestalLink, never()).remoteSupplyStop(any(), anyInt());
    }

    @Test
    void callerWithoutTheAppRightIsRefused() {
        Userinfo reception = new Userinfo("qr-reception", "Reception", "qr-reception@example.org");
        reception.setStatus(UserinfoStatus.ACTIVE);
        reception.getRoles().add(userroleDao.findByName(UserroleName.RECEPTION.name()));
        userinfoDao.saveAndFlush(reception);

        assertThatThrownBy(() -> supervisor.startQrPay(Username.of("qr-reception"), "ORD-5", SOCKET_CODE, "g"))
                .isInstanceOf(AccessNotAllowedException.class);
    }

    @Test
    void orderReferenceStartsOnePaymentOnly() {
        supervisor.startQrPay(APP, "ORD-6", SOCKET_CODE, "guest-7");

        assertThatThrownBy(() -> supervisor.startQrPay(APP, "ORD-6", SOCKET_CODE, "guest-7"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void unknownSocketCodeIsNotFound() {
        assertThatThrownBy(() -> supervisor.startQrPay(APP, "ORD-7", "HRBAY*P999*1", "guest-7"))
                .isInstanceOf(ObjectNotFoundException.class);
    }

    @Test
    void startThePedestalRefusesEndsInError() {
        when(pedestalLink.remoteSupplyStart(any(), anyInt(), anyString())).thenReturn(false);

        QrPayProcessDTO started = supervisor.startQrPay(APP, "ORD-8", SOCKET_CODE, "guest-7");

        assertThat(started.state()).isEqualTo("ERROR");
        assertThat(started.errorCode()).isEqualTo("REMOTE_SUPPLY_START_FAILED");
        assertThat(started.errorText()).isEqualTo(QrPayErrorCode.REMOTE_SUPPLY_START_FAILED.getText());
        assertThat(controlUnit.startSupply(socket1, "QR" + started.processId(),
                SupplyRequest.start(new BigDecimal("100.5"), plugIn)).getStatus())
                .isEqualTo(ValidationStatus.CARD_INVALID);
    }

    @Test
    void socketSupplyingForACardIsUnavailableForAQrPayment() {
        Group club = groupManager.createChildGroup(groupService.fetchIssuerGroup("HR ISSUERS"), "QR CLUB", "Club");
        groupManager.addAcceptanceAgreement(club, pedestal.getGroup());
        cards.card("8201", "HR-8201", cards.guest("Kovac", club), LocalDate.now().minusDays(3),
                LocalDate.now().plusDays(3));
        controlUnit.startSupply(socket1, "8201", SupplyRequest.start(new BigDecimal("100.5"), plugIn));

        QrPayProcessDTO started = supervisor.startQrPay(APP, "ORD-9", SOCKET_CODE, "guest-7");

        assertThat(started.state()).isEqualTo("ERROR");
        assertThat(started.errorCode()).isEqualTo("SOCKET_UNAVAILABLE");
        verify(pedestalLink, never()).remoteSupplyStart(any(), anyInt(), anyString());
    }

    @Test
    void secondPaymentAtASocketWaitingForTheGuestIsUnavailable() {
        supervisor.startQrPay(APP, "ORD-10", SOCKET_CODE, "guest-7");

        assertThat(supervisor.startQrPay(APP, "ORD-11", SOCKET_CODE, "guest-8").errorCode())
                .isEqualTo("SOCKET_UNAVAILABLE");
    }

    @Test
    void qrAuthTagIsAcceptedOnlyAtTheSocketOfThePayment() {
        QrPayProcessDTO started = supervisor.startQrPay(APP, "ORD-12", SOCKET_CODE, "guest-7");

        assertThat(controlUnit.startSupply(SocketLinkId.of("HR-BAY-P201", 2), "QR" + started.processId(),
                SupplyRequest.start(new BigDecimal("100.5"), plugIn)).getStatus())
                .isEqualTo(ValidationStatus.CARD_INVALID);
    }

    @Test
    void stopBeforeTheSupplyStartedEndsInError() {
        supervisor.startQrPay(APP, "ORD-13", SOCKET_CODE, "guest-7");

        QrPayProcessDTO stopped = supervisor.stopQrPay(APP, "ORD-13", "guest-7");

        assertThat(stopped.state()).isEqualTo("ERROR");
        assertThat(stopped.errorCode()).isEqualTo("SUPPLY_NOT_STARTED");
    }

    @Test
    void stopThePedestalDidNotGetCanBeRepeated() {
        QrPayProcessDTO started = supervisor.startQrPay(APP, "ORD-14", SOCKET_CODE, "guest-7");
        SupplySession session = startSupply(started);
        supervisor.onSupplyStarted(started.processId(), session.getId(), session.getTransactionId());
        when(pedestalLink.remoteSupplyStop(any(), anyInt())).thenReturn(false);

        assertThat(supervisor.stopQrPay(APP, "ORD-14", "guest-7").errorCode()).isEqualTo("REMOTE_SUPPLY_STOP_FAILED");

        when(pedestalLink.remoteSupplyStop(any(), anyInt())).thenReturn(true);
        assertThat(supervisor.stopQrPay(APP, "ORD-14", "guest-7").state()).isEqualTo("SOCKET_STOP_WAITING");
    }

    @Test
    void repeatedStopOfTheAppIsIgnored() {
        QrPayProcessDTO started = supervisor.startQrPay(APP, "ORD-15", SOCKET_CODE, "guest-7");
        SupplySession session = startSupply(started);
        supervisor.onSupplyStarted(started.processId(), session.getId(), session.getTransactionId());
        supervisor.stopQrPay(APP, "ORD-15", "guest-7");

        assertThat(supervisor.stopQrPay(APP, "ORD-15", "guest-7").state()).isEqualTo("SOCKET_STOP_WAITING");
        verify(pedestalLink).remoteSupplyStop(any(), anyInt());
    }

    @Test
    void signalTheStateDoesNotSupportPutsThePaymentInError() {
        QrPayProcessDTO started = supervisor.startQrPay(APP, "ORD-16", SOCKET_CODE, "guest-7");

        assertThatThrownBy(() -> stateFactory.createState(started.processId()).close())
                .isInstanceOf(IllegalStateException.class);
        QrPayProcess current = qrPayProcessService.findCurrent(started.processId());
        assertThat(current.getState()).isEqualTo("ERROR");
        assertThat(current.getErrorCode()).isEqualTo(QrPayErrorCode.UNSUPPORTED_OPERATION);
    }

    @Test
    void transitionOfARowThatIsNotCurrentAnyMoreIsRefused() {
        QrPayProcessDTO started = supervisor.startQrPay(APP, "ORD-17", SOCKET_CODE, "guest-7");
        QrPayProcess first = qrPayProcessDao.findByProcessIdOrderById(started.processId()).get(0);

        assertThatThrownBy(() -> qrPayProcessService.transAndSave(QrPayStateName.ERROR, first.detachedCopy()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void finishedChainsOlderThanTheDelayMoveToTheArchive() {
        QrPayProcessDTO error = supervisor.startQrPay(APP, "ORD-18", SOCKET_CODE, "guest-7");
        supervisor.stopQrPay(APP, "ORD-18", "guest-7");
        QrPayProcessDTO open = supervisor.startQrPay(APP, "ORD-19", "HRBAY*P201*1", "guest-8");
        int rowsOfError = qrPayProcessDao.findByProcessIdOrderById(error.processId()).size();

        assertThat(qrPayProcessService.archiveFinishedProcesses()).isZero();

        dateService.setClock(Clock.fixed(LocalDateTime.now().plusHours(25).atZone(ZoneId.systemDefault())
                .toInstant(), ZoneId.systemDefault()));
        assertThat(qrPayProcessService.archiveFinishedProcesses()).isEqualTo(rowsOfError);

        assertThat(qrPayProcessDao.findByProcessIdOrderById(error.processId())).isEmpty();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM QRPAY_PROCESS_ARCHIVE WHERE PROCESS_ID = ?",
                Integer.class, error.processId())).isEqualTo(rowsOfError);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM QRPAY_PROCESS_ARCHIVE WHERE PROCESS_ID = ? AND NEXT_PROCESS_ID IS NULL"
                        + " AND STATE = 'ERROR'", Integer.class, error.processId())).isEqualTo(1);
        assertThat(qrPayProcessService.findCurrent(open.processId())).isNotNull();
    }

    private SupplySession startSupply(QrPayProcessDTO started) {
        SupplyResponse response = controlUnit.startSupply(socket1, "QR" + started.processId(),
                SupplyRequest.start(new BigDecimal("100.5"), plugIn));
        return sessionManager.findSessionFor("HR-BAY-P201", response.getTransactionId());
    }

    private Socket socket(int socketNo) {
        return pedestalService.findSocket("HR-BAY-P201", socketNo);
    }

    private void appClient(String username) {
        Userinfo user = new Userinfo(username, username, username + "@example.org");
        user.setStatus(UserinfoStatus.ACTIVE);
        user.setGroup(groupService.fetchStaffGroup("HR STAFF"));
        user.getRoles().add(userroleDao.findByName(UserroleName.QRPAY_APP_CLIENT.name()));
        userinfoDao.saveAndFlush(user);
    }
}
