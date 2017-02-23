package io.pillopl.demo;

import io.pillopl.dto.pedestallink.v1.RemoteCommandRequest;
import io.pillopl.dto.pedestallink.v1.RemoteCommandResponse;
import io.pillopl.dto.pedestallink.v1.SendLocalListResponse;
import io.pillopl.dto.pedestallink.v1.SupplyStartedRequest;
import io.pillopl.dto.pedestallink.v1.SupplyStoppedRequest;
import io.pillopl.infrastructure.PedestalLinkVersion;
import io.pillopl.pedestallink.v2.PedestalLinkServiceV2;
import io.pillopl.pedestallink.v3.PedestalLinkServiceV3;
import io.pillopl.pedestallink.v3.PedestalLinkV3Handler;
import io.pillopl.util.DateService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Demo pedestals (profile {@code demo} only): the demo seed points the link address of its link v2 and v3 pedestals
 * here, so that the remote commands of the console reach a controller. Like a real controller it accepts the command
 * at once and reports the result a moment later over the inbound link: a remote stop ends the session with the
 * current meter value (reason REMOTE), a remote start opens a session for the card. Status requests, breaker resets
 * and local card lists are accepted without a report. Not part of the OpenAPI description (a demo stand-in).
 */
@RestController
@Hidden
@Profile("demo")
@RequestMapping(DemoPedestalSimulator.PATH + "/{pedestalCode}")
public class DemoPedestalSimulator {

    /**
     * Below the pedestal link paths, which are open and without CSRF (the link has no user session).
     */
    public static final String PATH = "/api/pedestal-link/demo-simulator";

    static final String REMOTE_REASON = "REMOTE";

    private static final Logger LOG = LoggerFactory.getLogger(DemoPedestalSimulator.class);

    private static final RemoteCommandResponse ACCEPTED = new RemoteCommandResponse(RemoteCommandResponse.ACCEPTED);

    private final JdbcTemplate jdbcTemplate;

    private final PedestalLinkServiceV2 linkServiceV2;

    private final PedestalLinkServiceV3 linkServiceV3;

    private final DateService dateService;

    private final long reportDelayInMillis;

    private final ScheduledExecutorService reports = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "demo-pedestal-simulator");
        thread.setDaemon(true);
        return thread;
    });

    public DemoPedestalSimulator(JdbcTemplate jdbcTemplate, PedestalLinkServiceV2 linkServiceV2,
                                 PedestalLinkServiceV3 linkServiceV3, DateService dateService,
                                 @Value("${campsite.demo.simulator.report-delay-in-millis:1500}")
                                 long reportDelayInMillis) {
        this.jdbcTemplate = jdbcTemplate;
        this.linkServiceV2 = linkServiceV2;
        this.linkServiceV3 = linkServiceV3;
        this.dateService = dateService;
        this.reportDelayInMillis = reportDelayInMillis;
    }

    @PostMapping("/requestSupplyStop")
    public RemoteCommandResponse requestSupplyStop(@PathVariable String pedestalCode,
                                                   @RequestBody RemoteCommandRequest request) {
        LOG.info(String.format("Demo pedestal %s: supply stop of transaction %s", pedestalCode,
                request.transactionId()));
        later(() -> reportSupplyStopped(pedestalCode, request.transactionId()));
        return ACCEPTED;
    }

    @PostMapping("/requestSupplyStart")
    public RemoteCommandResponse requestSupplyStart(@PathVariable String pedestalCode,
                                                    @RequestBody RemoteCommandRequest request) {
        LOG.info(String.format("Demo pedestal %s: supply start of socket %s", pedestalCode, request.socketNo()));
        later(() -> reportSupplyStarted(pedestalCode, request.socketNo(), request.authTag()));
        return ACCEPTED;
    }

    @PostMapping("/requestStatus")
    public RemoteCommandResponse requestStatus(@PathVariable String pedestalCode,
                                               @RequestBody RemoteCommandRequest request) {
        LOG.info(String.format("Demo pedestal %s: status request of socket %s", pedestalCode, request.socketNo()));
        return ACCEPTED;
    }

    @PostMapping("/resetBreaker")
    public RemoteCommandResponse resetBreaker(@PathVariable String pedestalCode,
                                              @RequestBody RemoteCommandRequest request) {
        LOG.info(String.format("Demo pedestal %s: breaker reset of socket %s", pedestalCode, request.socketNo()));
        return ACCEPTED;
    }

    @PostMapping("/localList")
    public SendLocalListResponse localList(@PathVariable String pedestalCode, @RequestBody Map<String, Object> list) {
        LOG.info(String.format("Demo pedestal %s: local card list version %s", pedestalCode, list.get("listVersion")));
        return new SendLocalListResponse(SendLocalListResponse.ACCEPTED);
    }

    /**
     * The stop message of the controller: the session ends with the socket's meter value.
     */
    void reportSupplyStopped(String pedestalCode, Integer transactionId) {
        BigDecimal meterKwh = meterOfTransaction(pedestalCode, transactionId);
        if (meterKwh == null) {
            LOG.info(String.format("Demo pedestal %s: transaction %s is not running", pedestalCode, transactionId));
            return;
        }
        SupplyStoppedRequest stopped = new SupplyStoppedRequest(transactionId, null, toWh(meterKwh),
                dateService.getNow(), REMOTE_REASON);
        if (linkVersionOf(pedestalCode) == PedestalLinkVersion.V3) {
            linkServiceV3.handleRequest(PedestalLinkV3Handler.SupplyStopped.class, pedestalCode, stopped);
        } else {
            linkServiceV2.supplyStopped(pedestalCode, stopped);
        }
    }

    /**
     * The start message of the controller for the card of the remote start.
     */
    void reportSupplyStarted(String pedestalCode, Integer socketNo, String authTag) {
        BigDecimal meterKwh = meterOfSocket(pedestalCode, socketNo);
        SupplyStartedRequest started = new SupplyStartedRequest(socketNo, authTag,
                toWh(meterKwh != null ? meterKwh : BigDecimal.ZERO), dateService.getNow());
        if (linkVersionOf(pedestalCode) == PedestalLinkVersion.V3) {
            linkServiceV3.handleRequest(PedestalLinkV3Handler.SupplyStarted.class, pedestalCode, started);
        } else {
            linkServiceV2.supplyStarted(pedestalCode, started);
        }
    }

    private void later(Runnable report) {
        reports.schedule(() -> {
            try {
                report.run();
            } catch (RuntimeException e) {
                LOG.warn("Demo pedestal report failed: " + e.getMessage(), e);
            }
        }, reportDelayInMillis, TimeUnit.MILLISECONDS);
    }

    private BigDecimal meterOfTransaction(String pedestalCode, Integer transactionId) {
        return single("SELECT so.METER_KWH FROM SUPPLY_SESSION s JOIN SOCKET so ON so.ID = s.SOCKET_ID "
                + "JOIN PEDESTAL p ON p.ID = so.PEDESTAL_ID WHERE p.CODE = ? AND s.TRANSACTION_ID = ? AND s.ACTIVE",
                BigDecimal.class, pedestalCode, transactionId);
    }

    private BigDecimal meterOfSocket(String pedestalCode, Integer socketNo) {
        return single("SELECT so.METER_KWH FROM SOCKET so JOIN PEDESTAL p ON p.ID = so.PEDESTAL_ID "
                + "WHERE p.CODE = ? AND so.SOCKET_NO = ? AND NOT so.DELETED", BigDecimal.class, pedestalCode, socketNo);
    }

    private PedestalLinkVersion linkVersionOf(String pedestalCode) {
        String version = single("SELECT t.LINK_VERSION FROM PEDESTAL p JOIN PEDESTAL_TYPE t "
                + "ON t.ID = p.PEDESTAL_TYPE_ID WHERE p.CODE = ?", String.class, pedestalCode);
        return version != null ? PedestalLinkVersion.valueOf(version) : PedestalLinkVersion.V2;
    }

    private <T> T single(String sql, Class<T> type, Object... args) {
        try {
            return jdbcTemplate.queryForObject(sql, type, args);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private static Integer toWh(BigDecimal kwh) {
        return kwh.multiply(BigDecimal.valueOf(1000)).intValue();
    }

    @PreDestroy
    void shutdown() {
        reports.shutdownNow();
    }
}
