package io.pillopl.monitoring;

import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.group.GroupType;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.Socket;
import io.pillopl.infrastructure.SocketStatus;
import io.pillopl.persistence.dao.SocketStatusLogDao;
import io.pillopl.persistence.dao.SocketStatusLogDao.StatusReportRow;
import io.pillopl.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@Transactional
class SocketStatusLogDaoIntegrationTest extends IntegrationTest {

    static final LocalDateTime JULY = LocalDateTime.of(2026, 7, 10, 8, 0);

    @Autowired SocketStatusLogDao socketStatusLogDao;
    @Autowired GroupService groupService;
    @Autowired GroupManager groupManager;
    @Autowired InfrastructureFixture fixture;
    @Autowired JdbcTemplate jdbcTemplate;

    Group bay, cove;
    Socket socket1, socket2, coveSocket;

    @BeforeEach
    void setUp() {
        Group hrSites = groupService.fetchSiteGroup("HR SITES");
        bay = groupManager.createChildGroup(hrSites, "BAY CAMPSITE", "Bay", GroupType.SITE);
        cove = groupManager.createChildGroup(hrSites, "COVE CAMPSITE", "Cove", GroupType.SITE);
        Pedestal pedestal = fixture.pedestal("HR-BAY-P001", bay, 2);
        socket1 = pedestal.findSocketBySocketNo(1);
        socket2 = pedestal.findSocketBySocketNo(2);
        coveSocket = fixture.pedestal("HR-COVE-P001", cove, 1).findSocketBySocketNo(1);
    }

    @Test
    void lastLogOfASocketIsTheNewestOne() {
        log(socket1, JULY, SocketStatus.AVAILABLE, "10");
        SocketStatusLog newest = log(socket1, JULY.plusMinutes(5), SocketStatus.SUPPLYING, "10");
        log(socket2, JULY.plusMinutes(9), SocketStatus.FAULTED, "0");

        assertThat(socketStatusLogDao.findLastBySocketId(socket1.getId())).isEqualTo(newest);
        assertThat(socketStatusLogDao.findLastBySocketId(coveSocket.getId())).isNull();
    }

    @Test
    void errorMaskOfTheManufacturerWinsOverTheProtocolMask() {
        jdbcTemplate.update("INSERT INTO PEDESTAL_ERROR_CODE (MANUFACTURER, CODE, MASK, MASKED, ABBREVIATION,"
                + " DESCRIPTION) VALUES (?, 'BreakerTripped', 256, TRUE, 'MCB', 'Miniature circuit breaker')",
                InfrastructureFixture.MANUFACTURER);

        assertThat(socketStatusLogDao.findErrorMask(InfrastructureFixture.MANUFACTURER, "BreakerTripped"))
                .isEqualTo(256L);
        assertThat(socketStatusLogDao.findErrorMask("OTHER PEDESTALS", "BreakerTripped")).isEqualTo(1L);
        assertThat(socketStatusLogDao.findErrorMask("OTHER PEDESTALS", "NoSuchCode")).isNull();
    }

    @Test
    void errorFlagsAreReadBackWithTheMasksOfTheProtocolWhenTheManufacturerHasNone() {
        assertThat(socketStatusLogDao.findErrorAbbreviations("OTHER PEDESTALS", 1L | 4L))
                .containsExactly("BRK", "TMP");
        assertThat(socketStatusLogDao.findErrorAbbreviations("OTHER PEDESTALS", 0L)).containsExactly("OK");
        assertThat(socketStatusLogDao.findErrorAbbreviations("OTHER PEDESTALS", 4611686018427387904L))
                .containsExactly("UNK");
    }

    @Test
    void errorFlagsOfAManufacturerWithOwnMasksUseOnlyItsMasks() {
        jdbcTemplate.update("INSERT INTO PEDESTAL_ERROR_CODE (MANUFACTURER, CODE, MASK, MASKED, ABBREVIATION,"
                + " DESCRIPTION) VALUES (?, 'BreakerTripped', 1, TRUE, 'MCB', 'Miniature circuit breaker')",
                InfrastructureFixture.MANUFACTURER);

        assertThat(socketStatusLogDao.findErrorAbbreviations(InfrastructureFixture.MANUFACTURER, 1L | 4L))
                .containsExactly("MCB");
    }

    @Test
    void statusReportSumsTheSecondsUpToTheNextLogPerStatus() {
        log(socket1, JULY, SocketStatus.AVAILABLE, "10");
        log(socket1, JULY.plusMinutes(10), SocketStatus.SUPPLYING, "10");
        log(socket1, JULY.plusMinutes(30), SocketStatus.SUPPLYING, "14");
        log(socket1, JULY.plusMinutes(40), SocketStatus.AVAILABLE, "14");
        log(socket1, JULY.plusMinutes(45), SocketStatus.FAULTED, "14");

        List<StatusReportRow> report = socketStatusLogDao.findStatusReport(LocalDate.of(2026, 7, 1),
                List.of(bay.getId()));

        assertThat(report).extracting(StatusReportRow::getStatus, StatusReportRow::getTimeInSeconds)
                .containsExactly(
                        tuple("AVAILABLE", 900L),
                        tuple("SUPPLYING", 1200L),
                        tuple("SUPPLYING_WITHOUT_CONSUMPTION", 600L));
        assertThat(report).allSatisfy(row -> {
            assertThat(row.getSocketId()).isEqualTo(socket1.getId());
            assertThat(row.getGroupId()).isEqualTo(bay.getId());
            assertThat(row.getSocketName()).isEqualTo("HR-BAY-P001_1");
        });
    }

    @Test
    void statusReportDoesNotCountTheTimeUpToTheFirstLogOfTheNextSocket() {
        log(socket1, JULY, SocketStatus.AVAILABLE, "10");
        log(socket1, JULY.plusMinutes(10), SocketStatus.FAULTED, "10");
        log(socket2, JULY.plusHours(5), SocketStatus.AVAILABLE, "3");
        log(socket2, JULY.plusHours(6), SocketStatus.SUPPLYING, "3");

        Map<String, Long> seconds = socketStatusLogDao.findStatusReport(LocalDate.of(2026, 7, 31),
                        List.of(bay.getId())).stream()
                .collect(Collectors.toMap(row -> row.getSocketId() + "/" + row.getStatus(),
                        StatusReportRow::getTimeInSeconds));

        assertThat(seconds).containsOnly(
                Map.entry(socket1.getId() + "/AVAILABLE", 600L),
                Map.entry(socket1.getId() + "/FAULTED", 0L),
                Map.entry(socket2.getId() + "/AVAILABLE", 3600L));
    }

    @Test
    void statusReportCoversOnlyTheMonthAndTheGroupsAsked() {
        log(socket1, JULY, SocketStatus.AVAILABLE, "10");
        log(socket1, JULY.plusMinutes(10), SocketStatus.FAULTED, "10");
        log(socket1, JULY.plusMonths(1), SocketStatus.AVAILABLE, "10");
        log(socket1, JULY.plusMonths(1).plusMinutes(20), SocketStatus.FAULTED, "10");
        log(coveSocket, JULY, SocketStatus.AVAILABLE, "1");
        log(coveSocket, JULY.plusMinutes(30), SocketStatus.FAULTED, "1");

        assertThat(socketStatusLogDao.findStatusReport(LocalDate.of(2026, 8, 15), List.of(bay.getId())))
                .extracting(StatusReportRow::getStatus, StatusReportRow::getTimeInSeconds)
                .containsExactly(tuple("AVAILABLE", 1200L));
        assertThat(socketStatusLogDao.findStatusReport(LocalDate.of(2026, 7, 15), List.of(cove.getId())))
                .extracting(StatusReportRow::getSocketId).containsOnly(coveSocket.getId());
    }

    @Test
    void statusReportViewShowsTheCurrentMonth() {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        LocalDateTime start = now.getDayOfMonth() > 1 ? now.withDayOfMonth(1).withHour(8).withMinute(0) : now;
        log(socket1, start, SocketStatus.AVAILABLE, "10");
        log(socket1, start.plusSeconds(30), SocketStatus.FAULTED, "10");
        log(socket1, start.minusMonths(1), SocketStatus.SUPPLYING, "10");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT STATUS, TIME_IN_SECONDS FROM V_SOCKET_STATUS_REPORT WHERE SOCKET_ID = ? ORDER BY STATUS",
                socket1.getId());

        assertThat(rows).extracting(row -> row.get("status"), row -> ((Number) row.get("time_in_seconds")).longValue())
                .contains(tuple("AVAILABLE", 30L))
                .noneMatch(row -> "SUPPLYING".equals(row.toList().get(0)));
    }

    @Test
    void utilizationMarksTheRowsWhereTheMeterMovedWhileSupplying() {
        SocketStatusLog idle = log(socket1, JULY, SocketStatus.AVAILABLE, "10");
        SocketStatusLog moving = log(socket1, JULY.plusMinutes(17), SocketStatus.SUPPLYING, "10");
        SocketStatusLog standing = log(socket1, JULY.plusMinutes(27), SocketStatus.SUPPLYING, "12.5");
        log(socket1, JULY.plusMinutes(57), SocketStatus.AVAILABLE, "12.5");

        socketStatusLogDao.refreshSocketUtilization();

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT ID, HOUR, MINUTES, SUPPLYING, TIME_IN_SECONDS, GROUP_ID FROM SOCKET_UTILIZATION"
                        + " WHERE SOCKET_ID = ? ORDER BY MOD_DATE", socket1.getId());
        assertThat(rows).hasSize(4);
        assertThat(rows.get(0)).containsEntry("id", idle.getId()).containsEntry("supplying", 0)
                .containsEntry("time_in_seconds", 1020L).containsEntry("group_id", bay.getId());
        assertThat(rows.get(1)).containsEntry("id", moving.getId()).containsEntry("supplying", 1)
                .containsEntry("hour", 8).containsEntry("minutes", 10);
        assertThat(rows.get(2)).containsEntry("id", standing.getId()).containsEntry("supplying", 0)
                .containsEntry("time_in_seconds", 1800L);
        assertThat(rows.get(3)).containsEntry("time_in_seconds", 0L);
    }

    private SocketStatusLog log(Socket socket, LocalDateTime at, SocketStatus status, String meterKwh) {
        SocketStatusLog log = new SocketStatusLog(socket, at, at);
        log.setStatus(status);
        log.setMeterKwh(new BigDecimal(meterKwh));
        return socketStatusLogDao.saveAndFlush(log);
    }
}
