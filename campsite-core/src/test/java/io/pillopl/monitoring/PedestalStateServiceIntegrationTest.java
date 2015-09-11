package io.pillopl.monitoring;

import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.group.GroupType;
import io.pillopl.group.attribute.Notification;
import io.pillopl.group.attribute.NotificationService;
import io.pillopl.group.attribute.NotificationType;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.PedestalState;
import io.pillopl.infrastructure.SocketStatus;
import io.pillopl.persistence.dao.PedestalStateDao;
import io.pillopl.persistence.dao.SocketStatusLogDao;
import io.pillopl.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class PedestalStateServiceIntegrationTest extends IntegrationTest {

    @Autowired PedestalStateService pedestalStateService;
    @Autowired PedestalStateDao pedestalStateDao;
    @Autowired SocketStatusLogDao socketStatusLogDao;
    @Autowired NotificationService notificationService;
    @Autowired GroupService groupService;
    @Autowired GroupManager groupManager;
    @Autowired InfrastructureFixture fixture;
    @Autowired JdbcTemplate jdbcTemplate;

    final LocalDateTime now = LocalDateTime.now();
    Group bay;

    @BeforeEach
    void setUp() {
        bay = groupManager.createChildGroup(groupService.fetchSiteGroup("HR SITES"), "BAY CAMPSITE", "Bay",
                GroupType.SITE);
        Notification notification = new Notification("Bay service", "service@bay.test",
                NotificationType.PEDESTAL_STATUS);
        notification.setGroup(bay);
        notificationService.persist(notification);
    }

    @AfterTransaction
    void removeAuditedMails() {
        jdbcTemplate.update("DELETE FROM AUDIT WHERE ACTION_TYPE = 'EMAIL' AND ACTION_NAME = 'service@bay.test'");
    }

    @Test
    void silentPedestalIsTimedOutOnceWithAMailToItsService() {
        PedestalState silent = state("HR-BAY-P001", now.minusMinutes(30), false);
        PedestalState talking = state("HR-BAY-P002", now.minusMinutes(2), false);
        PedestalState alreadyTimedOut = state("HR-BAY-P003", now.minusHours(5), true);

        PedestalStateService.Report report = pedestalStateService.timeoutPedestals(15);

        assertThat(report.getTimedOutPedestals()).isEqualTo(1);
        assertThat(report.getSentMails()).isEqualTo(1);
        assertThat(report.getSentEvents()).isEqualTo(1);
        assertThat(pedestalStateDao.findById(silent.getId()).orElseThrow().isTimeout()).isTrue();
        assertThat(silent.getLastTimeout()).isNotNull();
        assertThat(talking.isTimeout()).isFalse();
        assertThat(alreadyTimedOut.getLastTimeout()).isNull();
        assertThat(mailSubjects()).containsExactly("Pedestal HR-BAY-P001 in timeout");

        assertThat(pedestalStateService.timeoutPedestals(15).getTimedOutPedestals()).isZero();
    }

    @Test
    void pedestalsSilentForLongerThanTheUnknownTimeoutGetUnknownEventsEvenWhenTimedOut() {
        state("HR-BAY-P001", now.minusHours(4), false);
        state("HR-BAY-P002", now.minusHours(5), true);
        state("HR-BAY-P003", now.minusMinutes(30), false);
        PedestalState deleted = state("HR-BAY-P004", now.minusHours(6), true);
        deleted.getPedestal().setDeleted(true);

        PedestalStateService.Report report = pedestalStateService.sendUnknownEvents(180);

        assertThat(report.getSentEvents()).isEqualTo(2);
        assertThat(report.getSentMails()).isZero();
    }

    @Test
    void utilizationViewIsRefreshedFromTheMonitoringLog() {
        Pedestal pedestal = fixture.pedestal("HR-BAY-P001", bay, 1);
        SocketStatusLog log = new SocketStatusLog(pedestal.findSocketBySocketNo(1), now, now);
        log.setStatus(SocketStatus.SUPPLYING);
        socketStatusLogDao.saveAndFlush(log);

        pedestalStateService.refreshSocketUtilization();

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM SOCKET_UTILIZATION WHERE ID = ?", Long.class,
                log.getId())).isEqualTo(1L);
    }

    private PedestalState state(String code, LocalDateTime lastIncoming, boolean timeout) {
        Pedestal pedestal = fixture.pedestal(code, bay, 1);
        PedestalState state = new PedestalState(pedestal, lastIncoming);
        state.setTimeout(timeout);
        return pedestalStateDao.saveAndFlush(state);
    }

    private List<String> mailSubjects() {
        return jdbcTemplate.queryForList("SELECT INFO_FIELD_1 FROM AUDIT WHERE ACTION_TYPE = 'EMAIL'"
                + " AND ACTION_NAME = 'service@bay.test' ORDER BY ID", String.class);
    }
}
