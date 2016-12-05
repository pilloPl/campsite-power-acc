package io.pillopl.monitoring;

import io.pillopl.api.DashboardApi;
import io.pillopl.api.exception.ConflictException;
import io.pillopl.api.exception.InvalidInputException;
import io.pillopl.api.exception.ObjectNotFoundException;
import io.pillopl.config.SystemPropertyService;
import io.pillopl.dto.monitoring.AlertDTO;
import io.pillopl.dto.monitoring.DashboardDTO;
import io.pillopl.group.GroupService;
import io.pillopl.persistence.dao.OperationsViewDao;
import io.pillopl.persistence.dao.OperationsViewDao.AlertSource;
import io.pillopl.persistence.dao.OperationsViewDao.DashboardCounts;
import io.pillopl.persistence.dao.ConsoleAlertAckDao;
import io.pillopl.user.PrincipalResolver;
import io.pillopl.user.Userinfo;
import io.pillopl.user.UserrightName;
import io.pillopl.util.ApiExceptionTranslator;
import io.pillopl.util.DateService;
import io.pillopl.util.SiteScopeHelper;
import io.pillopl.value.Username;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dashboard of the console (PEDESTAL_VIEW). The key figures and the live alerts are read from the data of the
 * caller's SITE scope; failed events (CONFIG_EDIT) and refused ERP lines (BILLING_EDIT) are network wide and only
 * shown to callers with the right. An alert's key names its source and occurrence, so an acknowledgement covers that
 * occurrence only.
 */
@Service
@Transactional
public class DashboardApiService implements DashboardApi {

    private static final Logger LOG = LoggerFactory.getLogger(DashboardApiService.class);

    static final int DEFAULT_ROWS = 200;

    static final int MAX_ROWS = 1000;

    static final String SOCKET_FAULT = "SOCKET_FAULT";
    static final String PEDESTAL_TIMEOUT = "PEDESTAL_TIMEOUT";
    static final String STALE_SESSION = "STALE_SESSION";
    static final String REVIEW_RECORD = "REVIEW_RECORD";
    static final String FAILED_EVENT = "FAILED_EVENT";
    static final String ERP_ERROR = "ERP_ERROR";

    private final OperationsViewDao operationsViewDao;

    private final ConsoleAlertAckDao consoleAlertAckDao;

    private final GroupService groupService;

    private final SystemPropertyService systemPropertyService;

    private final DateService dateService;

    private final PrincipalResolver principalResolver;

    public DashboardApiService(OperationsViewDao operationsViewDao, ConsoleAlertAckDao consoleAlertAckDao,
                               GroupService groupService, SystemPropertyService systemPropertyService,
                               DateService dateService, PrincipalResolver principalResolver) {
        this.operationsViewDao = operationsViewDao;
        this.consoleAlertAckDao = consoleAlertAckDao;
        this.groupService = groupService;
        this.systemPropertyService = systemPropertyService;
        this.dateService = dateService;
        this.principalResolver = principalResolver;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardDTO getDashboard(Username authUsername, Long campsiteGroupId) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.PEDESTAL_VIEW);
            LocalDateTime now = dateService.getNow();
            Long siteGroupId = SiteScopeHelper.siteGroupIdOf(principal, campsiteGroupId, groupService);
            if (siteGroupId == null) {
                return new DashboardDTO(now, 0, BigDecimal.ZERO, 0, 0, 0, 0, 0, null, null, 0);
            }
            boolean withEvents = principal.hasRight(UserrightName.CONFIG_EDIT.name());
            boolean withErp = principal.hasRight(UserrightName.BILLING_EDIT.name());
            DashboardCounts counts = operationsViewDao.getDashboardCounts(siteGroupId, now.toLocalDate()
                    .atStartOfDay(), silentSince(now), withEvents, withErp);
            long openAlerts = alerts(siteGroupId, now, withEvents, withErp, MAX_ROWS).stream()
                    .filter(alert -> !alert.acknowledged()).count();
            return new DashboardDTO(now, counts.activeSessions(), counts.kwhToday(), counts.faultedSockets(),
                    counts.breakerTrippedSockets(), counts.timedOutPedestals(), counts.reviewQueue(),
                    counts.staleSessions(), counts.failedEvents(), counts.erpLinesInError(), openAlerts);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertDTO> findAlerts(Username authUsername, Long campsiteGroupId, boolean includeAcknowledged,
                                     Integer maxRows) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.PEDESTAL_VIEW);
            Long siteGroupId = SiteScopeHelper.siteGroupIdOf(principal, campsiteGroupId, groupService);
            if (siteGroupId == null) {
                return List.of();
            }
            List<AlertDTO> alerts = alerts(siteGroupId, dateService.getNow(),
                    principal.hasRight(UserrightName.CONFIG_EDIT.name()),
                    principal.hasRight(UserrightName.BILLING_EDIT.name()), rows(maxRows));
            return includeAcknowledged ? alerts : alerts.stream().filter(alert -> !alert.acknowledged()).toList();
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public AlertDTO acknowledgeAlert(Username authUsername, String alertKey, String comment) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.PEDESTAL_VIEW);
            if (alertKey == null || alertKey.isBlank()) {
                throw new InvalidInputException("Alert key is missing");
            }
            Long siteGroupId = SiteScopeHelper.siteGroupIdOf(principal, null, groupService);
            LocalDateTime now = dateService.getNow();
            AlertDTO alert = siteGroupId == null ? null : alerts(siteGroupId, now,
                    principal.hasRight(UserrightName.CONFIG_EDIT.name()),
                    principal.hasRight(UserrightName.BILLING_EDIT.name()), MAX_ROWS).stream()
                    .filter(candidate -> candidate.alertKey().equals(alertKey.trim())).findFirst().orElse(null);
            if (alert == null) {
                throw ObjectNotFoundException.of("Alert", alertKey);
            }
            if (alert.acknowledged()) {
                throw new ConflictException(String.format("Alert %s was acknowledged by %s", alertKey,
                        alert.acknowledgedBy()));
            }
            ConsoleAlertAck ack = consoleAlertAckDao.saveAndFlush(new ConsoleAlertAck(alert.alertKey(),
                    principal.getUsername(), now, comment));
            LOG.info(String.format("Alert %s acknowledged by %s", alert.alertKey(), principal.getUsername()));
            return withAck(alert, ack);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    private List<AlertDTO> alerts(Long siteGroupId, LocalDateTime now, boolean withEvents, boolean withErp,
                                  int maxRows) {
        List<AlertSource> sources = operationsViewDao.findAlertSources(siteGroupId, silentSince(now), withEvents,
                withErp, maxRows);
        List<AlertDTO> alerts = new ArrayList<>(sources.size());
        for (AlertSource source : sources) {
            alerts.add(toAlert(source));
        }
        if (alerts.isEmpty()) {
            return alerts;
        }
        Map<String, ConsoleAlertAck> acks = new HashMap<>();
        for (ConsoleAlertAck ack : consoleAlertAckDao.findByAlertKeyIn(alerts.stream().map(AlertDTO::alertKey)
                .toList())) {
            acks.put(ack.getAlertKey(), ack);
        }
        List<AlertDTO> result = new ArrayList<>(alerts.size());
        for (AlertDTO alert : alerts) {
            ConsoleAlertAck ack = acks.get(alert.alertKey());
            result.add(ack != null ? withAck(alert, ack) : alert);
        }
        return result;
    }

    /**
     * Key, severity and text of an alert source. Faults and timeouts are errors, stale sessions, failed events and
     * refused ERP lines warnings, records waiting for review information.
     */
    static AlertDTO toAlert(AlertSource source) {
        String severity;
        String text;
        switch (source.alertType()) {
            case SOCKET_FAULT -> {
                severity = "ERROR";
                text = String.format("Socket %s of pedestal %s: %s", source.socketName(), source.pedestalCode(),
                        source.detail());
            }
            case PEDESTAL_TIMEOUT -> {
                severity = "ERROR";
                text = String.format("Pedestal %s is silent since %s", source.pedestalCode(), source.detail());
            }
            case STALE_SESSION -> {
                severity = "WARNING";
                text = String.format("Supply session %d at socket %s of pedestal %s reports nothing since %s",
                        source.objectId(), source.socketName(), source.pedestalCode(), source.detail());
            }
            case REVIEW_RECORD -> {
                severity = "INFO";
                text = String.format("Consumption record %d of pedestal %s waits for review: %s", source.objectId(),
                        source.pedestalCode(), source.detail());
            }
            case FAILED_EVENT -> {
                severity = "WARNING";
                text = "Event not delivered: " + source.detail();
            }
            case ERP_ERROR -> {
                severity = "WARNING";
                text = "ERP refused the billing line " + source.detail();
            }
            default -> throw new IllegalArgumentException("Unknown alert type " + source.alertType());
        }
        return new AlertDTO(alertKey(source), source.alertType(), severity, source.alertTime(), text,
                source.pedestalCode(), source.socketId(), source.sessionId(), source.recordId(), source.objectId(),
                false, null, null, null);
    }

    /**
     * {@code <type>:<object id>:<epoch second of the occurrence>}.
     */
    static String alertKey(AlertSource source) {
        long epochSecond = source.alertTime() != null
                ? source.alertTime().atZone(ZoneId.systemDefault()).toEpochSecond() : 0L;
        return source.alertType() + ":" + source.objectId() + ":" + epochSecond;
    }

    private static AlertDTO withAck(AlertDTO alert, ConsoleAlertAck ack) {
        return new AlertDTO(alert.alertKey(), alert.alertType(), alert.severity(), alert.alertTime(), alert.text(),
                alert.pedestalCode(), alert.socketId(), alert.supplySessionId(), alert.consumptionRecordId(),
                alert.objectId(), true, ack.getAcknowledgedBy(), ack.getAckDate(), ack.getComment());
    }

    private LocalDateTime silentSince(LocalDateTime now) {
        return now.minusHours(systemPropertyService.getStaleSessionTimeoutInHours());
    }

    static int rows(Integer maxRows) {
        if (maxRows == null) {
            return DEFAULT_ROWS;
        }
        if (maxRows < 1 || maxRows > MAX_ROWS) {
            throw new InvalidInputException("Rows must be between 1 and " + MAX_ROWS + ": " + maxRows);
        }
        return maxRows;
    }
}
