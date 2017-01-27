package io.pillopl.demo;

import io.pillopl.dto.pedestallink.v1.RemoteCommandRequest;
import io.pillopl.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The demo pedestals answer the remote commands of the console: commands are accepted at once, a remote stop is
 * reported back over the inbound link of the pedestal's generation and ends the session.
 */
@ActiveProfiles({"integration-test", "demo"})
class DemoPedestalSimulatorIntegrationTest extends IntegrationTest {

    @Autowired DemoPedestalSimulator simulator;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void linkTwoAndThreePedestalsOfTheDemoAreServedByTheSimulatorAndLinkOnePedestalsHaveNoAddress() {
        List<Map<String, Object>> pedestals = jdbcTemplate.queryForList("SELECT p.CODE, p.LINK_URL, t.LINK_VERSION "
                + "FROM PEDESTAL p JOIN PEDESTAL_TYPE t ON t.ID = p.PEDESTAL_TYPE_ID WHERE p.ID >= 900000000");

        assertThat(pedestals).isNotEmpty().allSatisfy(pedestal -> {
            if ("V1".equals(pedestal.get("LINK_VERSION"))) {
                assertThat(pedestal.get("LINK_URL")).isNull();
            } else {
                assertThat((String) pedestal.get("LINK_URL")).startsWith("http://localhost:")
                        .endsWith(DemoPedestalSimulator.PATH + "/" + pedestal.get("CODE"));
            }
        });
    }

    @Test
    void remoteCommandsAreAcceptedAtOnce() {
        String pedestal = activeSessionOn("V2").code();

        assertThat(simulator.requestSupplyStop(pedestal, RemoteCommandRequest.stop(-1)).isAccepted()).isTrue();
        assertThat(simulator.requestStatus(pedestal, RemoteCommandRequest.forSocket(1)).isAccepted()).isTrue();
        assertThat(simulator.resetBreaker(pedestal, RemoteCommandRequest.forSocket(1)).isAccepted()).isTrue();
        assertThat(simulator.localList(pedestal, Map.of("listVersion", 3)).isAccepted()).isTrue();
    }

    @Test
    void reportedStopOfLinkTwoPedestalEndsTheSessionWithReasonRemote() {
        ActiveSession session = activeSessionOn("V2");

        simulator.reportSupplyStopped(session.code(), session.transactionId());

        assertThatSessionEndedRemotely(session);
    }

    @Test
    void reportedStopOfLinkThreePedestalEndsTheSessionWithReasonRemote() {
        ActiveSession session = activeSessionOn("V3");

        simulator.reportSupplyStopped(session.code(), session.transactionId());

        assertThatSessionEndedRemotely(session);
    }

    @Test
    void stopReportOfATransactionThatIsNotRunningChangesNothing() {
        ActiveSession session = activeSessionOn("V2");
        long active = activeSessions();

        simulator.reportSupplyStopped(session.code(), -1);

        assertThat(activeSessions()).isEqualTo(active);
    }

    private void assertThatSessionEndedRemotely(ActiveSession session) {
        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT ACTIVE, STOP_REASON FROM SUPPLY_SESSION WHERE ID = ?",
                session.id());
        assertThat(row.get("ACTIVE")).isEqualTo(false);
        assertThat(row.get("STOP_REASON")).isEqualTo(DemoPedestalSimulator.REMOTE_REASON);
    }

    private long activeSessions() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM SUPPLY_SESSION WHERE ACTIVE", Long.class);
    }

    private ActiveSession activeSessionOn(String linkVersion) {
        return jdbcTemplate.query("SELECT s.ID, s.TRANSACTION_ID, p.CODE FROM SUPPLY_SESSION s "
                        + "JOIN SOCKET so ON so.ID = s.SOCKET_ID JOIN PEDESTAL p ON p.ID = so.PEDESTAL_ID "
                        + "JOIN PEDESTAL_TYPE t ON t.ID = p.PEDESTAL_TYPE_ID "
                        + "WHERE s.ACTIVE AND t.LINK_VERSION = ? AND p.ID >= 900000000 ORDER BY s.ID LIMIT 1",
                (rs, i) -> new ActiveSession(rs.getLong("ID"), rs.getInt("TRANSACTION_ID"), rs.getString("CODE")),
                linkVersion).stream().findFirst().orElseThrow(() -> new AssertionError(
                "The demo has no active session on a link " + linkVersion + " pedestal"));
    }

    record ActiveSession(long id, int transactionId, String code) {
    }
}
