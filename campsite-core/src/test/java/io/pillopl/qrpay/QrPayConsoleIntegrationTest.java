package io.pillopl.qrpay;

import io.pillopl.api.exception.AccessNotAllowedException;
import io.pillopl.api.exception.ConflictException;
import io.pillopl.api.exception.ObjectNotFoundException;
import io.pillopl.commons.DateRange;
import io.pillopl.commons.ListPage;
import io.pillopl.commons.Pagination;
import io.pillopl.dto.qrpay.QrPayProcessDTO;
import io.pillopl.dto.qrpay.QrPayProcessOverviewDTO;
import io.pillopl.dto.qrpay.QrPayTimelineDTO;
import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.PedestalService;
import io.pillopl.infrastructure.Socket;
import io.pillopl.pedestallink.v1.PedestalLinkClientV1;
import io.pillopl.persistence.dao.SocketDao;
import io.pillopl.persistence.dao.UserinfoDao;
import io.pillopl.persistence.dao.UserroleDao;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * The QR payments of the console on the database: the list of the live and archived processes in the SITE scope of
 * the caller (hand-written SQL), the timeline of a chain and the cancel of stuck processes through the states.
 */
@Transactional
class QrPayConsoleIntegrationTest extends IntegrationTest {

    static final String SOCKET_CODE = "HRPORT*P301*1";
    static final Username APP = Username.of("console-payment-app");
    static final Username OFFICE = Username.of("qr-office");

    @MockitoBean PedestalLinkClientV1 pedestalLink;

    @Autowired QrPayProcessSupervisor supervisor;
    @Autowired SupplyControlUnitImpl controlUnit;
    @Autowired SupplySessionManager sessionManager;
    @Autowired PedestalService pedestalService;
    @Autowired SocketDao socketDao;
    @Autowired UserinfoDao userinfoDao;
    @Autowired UserroleDao userroleDao;
    @Autowired GroupService groupService;
    @Autowired GroupManager groupManager;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired DateService dateService;

    final SocketLinkId socket1 = SocketLinkId.of("HR-PORT-P301", 1);
    final LocalDateTime plugIn = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).minusMinutes(5);
    Group harbour;

    @BeforeEach
    void setUp() {
        harbour = groupManager.createChildGroup(groupService.fetchSiteGroup("HR SITES"), "HARBOUR QR", "Harbour");
        Pedestal pedestal = infrastructure.pedestal("HR-PORT-P301", harbour, 2);
        pedestal.setBillable(true);
        Socket socket = pedestalService.findSocket("HR-PORT-P301", 1);
        socket.setSocketCode(SOCKET_CODE);
        socketDao.saveAndFlush(socket);
        user(APP.value(), UserroleName.QRPAY_APP_CLIENT, null);
        user(OFFICE.value(), UserroleName.BACK_OFFICE, groupService.fetchSiteGroup("HR SITES"));
        user("qr-it-office", UserroleName.BACK_OFFICE, groupService.fetchSiteGroup("IT SITES"));
        user("qr-desk", UserroleName.RECEPTION, groupService.fetchSiteGroup("HR SITES"));
        supervisor.setSignalRetryPauseInMillis(0);
        when(pedestalLink.remoteSupplyStart(any(), anyInt(), anyString())).thenReturn(true);
        when(pedestalLink.remoteSupplyStop(any(), anyInt())).thenReturn(true);
    }

    @AfterEach
    void resetClock() {
        dateService.setClock(null);
    }

    @Test
    void consoleListsTheCurrentStateOfThePaymentsInTheSiteScopeNewestFirst() {
        QrPayProcessDTO first = supervisor.startQrPay(APP, "PORT-1", SOCKET_CODE, "guest-1");
        supervisor.stopQrPay(APP, "PORT-1", "guest-1");
        QrPayProcessDTO second = supervisor.startQrPay(APP, "PORT-2", SOCKET_CODE, "guest-2");

        ListPage<QrPayProcessOverviewDTO> all = supervisor.findQrPayProcesses(OFFICE, null, harbour.getId(),
                DateRange.UNBOUNDED, Pagination.FIRST_PAGE);
        assertThat(all.total()).isEqualTo(2);
        assertThat(all.items()).extracting(QrPayProcessOverviewDTO::processId)
                .containsExactly(second.processId(), first.processId());
        QrPayProcessOverviewDTO open = all.items().get(0);
        assertThat(open.state()).isEqualTo("SOCKET_START_WAITING");
        assertThat(open.socketCode()).isEqualTo(SOCKET_CODE);
        assertThat(open.pedestalCode()).isEqualTo("HR-PORT-P301");
        assertThat(open.campsiteName()).isEqualTo("Harbour");
        assertThat(open.stuck()).isFalse();
        assertThat(all.items().get(1).errorCode()).isEqualTo("SUPPLY_NOT_STARTED");

        assertThat(supervisor.findQrPayProcesses(OFFICE, "error", null, DateRange.UNBOUNDED, null).items())
                .extracting(QrPayProcessOverviewDTO::orderReference).contains("PORT-1").doesNotContain("PORT-2");
        assertThat(supervisor.findQrPayProcesses(OFFICE, null, harbour.getId(),
                new DateRange(LocalDateTime.now().plusDays(1), null), null).items()).isEmpty();
        assertThat(supervisor.findQrPayProcesses(OFFICE, null, harbour.getId(), DateRange.UNBOUNDED,
                new Pagination(1, 1)).items()).extracting(QrPayProcessOverviewDTO::processId)
                .containsExactly(first.processId());
        assertThat(supervisor.findQrPayProcesses(Username.of("qr-it-office"), null, null, DateRange.UNBOUNDED,
                null).items()).extracting(QrPayProcessOverviewDTO::orderReference).doesNotContain("PORT-1", "PORT-2");
    }

    @Test
    void listAndTimelineNeedTheReportRightAndTheScope() {
        QrPayProcessDTO started = supervisor.startQrPay(APP, "PORT-3", SOCKET_CODE, "guest-1");

        assertThatThrownBy(() -> supervisor.findQrPayProcesses(Username.of("qr-desk"), null, null,
                DateRange.UNBOUNDED, null)).isInstanceOf(AccessNotAllowedException.class);
        assertThatThrownBy(() -> supervisor.getQrPayTimeline(Username.of("qr-it-office"), started.processId()))
                .isInstanceOf(ObjectNotFoundException.class);
        assertThatThrownBy(() -> supervisor.getQrPayTimeline(OFFICE, 987_654_321L))
                .isInstanceOf(ObjectNotFoundException.class);
        assertThatThrownBy(() -> supervisor.findQrPayProcesses(OFFICE, "SLEEPING", null, DateRange.UNBOUNDED, null))
                .isInstanceOf(io.pillopl.api.exception.InvalidInputException.class);
    }

    @Test
    void timelineShowsEveryStateOfTheChainWithTheStateMachine() {
        QrPayProcessDTO started = supervisor.startQrPay(APP, "PORT-4", SOCKET_CODE, "guest-1");
        SupplySession session = startSupply(started);
        supervisor.onSupplyStarted(started.processId(), session.getId(), session.getTransactionId());

        QrPayTimelineDTO timeline = supervisor.getQrPayTimeline(OFFICE, started.processId());

        assertThat(timeline.orderReference()).isEqualTo("PORT-4");
        assertThat(timeline.currentState()).isEqualTo("START_CONFIRMED");
        assertThat(timeline.steps()).extracting(QrPayTimelineDTO.Step::state).containsExactly("START_REQUESTED",
                "SOCKET_START_WAITING", "SOCKET_START_RECEIVED", "START_CONFIRMED");
        assertThat(timeline.steps().get(0).until()).isNotNull();
        assertThat(timeline.steps().get(3).until()).isNull();
        assertThat(timeline.states()).hasSize(QrPayStateName.values().length);
        assertThat(timeline.transitions()).contains(new QrPayTimelineDTO.Transition("START_CONFIRMED",
                "STOP_REQUESTED"));
        assertThat(timeline.cancellable()).as("not stuck yet").isFalse();
    }

    @Test
    void stuckRunningPaymentIsCancelledByStoppingItsSupply() {
        QrPayProcessDTO started = supervisor.startQrPay(APP, "PORT-5", SOCKET_CODE, "guest-1");
        SupplySession session = startSupply(started);
        supervisor.onSupplyStarted(started.processId(), session.getId(), session.getTransactionId());

        assertThatThrownBy(() -> supervisor.cancelQrPayProcess(OFFICE, started.processId()))
                .isInstanceOf(ConflictException.class).hasMessageContaining("not stuck");

        laterBy(31);
        assertThat(supervisor.findQrPayProcesses(OFFICE, null, harbour.getId(), DateRange.UNBOUNDED, null)
                .items().get(0).stuck()).isTrue();
        QrPayTimelineDTO cancelled = supervisor.cancelQrPayProcess(OFFICE, started.processId());

        assertThat(cancelled.currentState()).isEqualTo("SOCKET_STOP_WAITING");
        assertThat(cancelled.steps()).extracting(QrPayTimelineDTO.Step::state).endsWith("STOP_REQUESTED",
                "SOCKET_STOP_WAITING");
        org.mockito.Mockito.verify(pedestalLink).remoteSupplyStop(any(), anyInt());
    }

    @Test
    void stuckPaymentWithoutSupplyGoesToErrorAndCannotBeCancelledAgain() {
        QrPayProcessDTO started = supervisor.startQrPay(APP, "PORT-6", SOCKET_CODE, "guest-1");
        laterBy(45);

        QrPayTimelineDTO cancelled = supervisor.cancelQrPayProcess(OFFICE, started.processId());

        assertThat(cancelled.currentState()).isEqualTo("ERROR");
        assertThat(cancelled.steps().get(cancelled.steps().size() - 1).errorCode()).isEqualTo("CANCELLED");
        assertThat(cancelled.steps().get(cancelled.steps().size() - 1).errorMessage())
                .isEqualTo("Cancelled by qr-office");
        assertThat(cancelled.cancellable()).isFalse();
        assertThatThrownBy(() -> supervisor.cancelQrPayProcess(OFFICE, started.processId()))
                .isInstanceOf(ConflictException.class).hasMessageContaining("no supply runs");
        assertThatThrownBy(() -> supervisor.cancelQrPayProcess(Username.of("qr-desk"), started.processId()))
                .isInstanceOf(AccessNotAllowedException.class);
    }

    @Test
    void archiveRunMovesFinishedChainsAndTheListStillShowsThem() {
        QrPayProcessDTO failed = supervisor.startQrPay(APP, "PORT-7", SOCKET_CODE, "guest-1");
        supervisor.stopQrPay(APP, "PORT-7", "guest-1");
        laterBy(25 * 60);

        assertThat(supervisor.runQrPayArchive(OFFICE)).isGreaterThan(0);

        QrPayOverviewAssert.assertArchived(supervisor.findQrPayProcesses(OFFICE, null, harbour.getId(),
                DateRange.UNBOUNDED, null), failed.processId());
        QrPayTimelineDTO timeline = supervisor.getQrPayTimeline(OFFICE, failed.processId());
        assertThat(timeline.archived()).isTrue();
        assertThat(timeline.currentState()).isEqualTo("ERROR");
        assertThatThrownBy(() -> supervisor.cancelQrPayProcess(OFFICE, failed.processId()))
                .isInstanceOf(ConflictException.class).hasMessageContaining("archived");
        assertThatThrownBy(() -> supervisor.runQrPayArchive(Username.of("qr-desk")))
                .isInstanceOf(AccessNotAllowedException.class);
    }

    private void laterBy(long minutes) {
        dateService.setClock(Clock.fixed(LocalDateTime.now().plusMinutes(minutes).atZone(ZoneId.systemDefault())
                .toInstant(), ZoneId.systemDefault()));
    }

    private SupplySession startSupply(QrPayProcessDTO started) {
        SupplyResponse response = controlUnit.startSupply(socket1, "QR" + started.processId(),
                SupplyRequest.start(new BigDecimal("100.5"), plugIn));
        return sessionManager.findSessionFor("HR-PORT-P301", response.getTransactionId());
    }

    private void user(String username, UserroleName role, Group siteScope) {
        Userinfo user = new Userinfo(username, username, username + "@example.org");
        user.setStatus(UserinfoStatus.ACTIVE);
        user.setGroup(groupService.fetchStaffGroup("HR STAFF"));
        if (siteScope != null) {
            user.getGroups().add(siteScope);
        }
        user.getRoles().add(userroleDao.findByName(role.name()));
        userinfoDao.saveAndFlush(user);
    }

    static final class QrPayOverviewAssert {

        static void assertArchived(ListPage<QrPayProcessOverviewDTO> page, Long processId) {
            assertThat(page.items()).filteredOn(p -> p.processId().equals(processId)).singleElement()
                    .satisfies(p -> {
                        assertThat(p.archived()).isTrue();
                        assertThat(p.state()).isEqualTo("ERROR");
                    });
        }
    }
}
