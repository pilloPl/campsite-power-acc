package io.pillopl.monitoring;

import io.pillopl.api.MonitoringApi;
import io.pillopl.api.exception.InvalidInputException;
import io.pillopl.api.exception.ObjectNotFoundException;
import io.pillopl.commons.DateRange;
import io.pillopl.dto.infrastructure.PedestalCommandDTO;
import io.pillopl.dto.monitoring.CommunicationLogDTO;
import io.pillopl.dto.monitoring.FailedEventDTO;
import io.pillopl.dto.monitoring.HeartbeatTimeoutDTO;
import io.pillopl.dto.monitoring.SocketStatusLogDTO;
import io.pillopl.dto.monitoring.StatusReportLineDTO;
import io.pillopl.event.EventRetry;
import io.pillopl.group.GroupService;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.PedestalService;
import io.pillopl.infrastructure.PedestalState;
import io.pillopl.infrastructure.Socket;
import io.pillopl.persistence.dao.SocketStatusLogDao;
import io.pillopl.supply.SupplySessionMapper;
import io.pillopl.user.PrincipalResolver;
import io.pillopl.user.Userinfo;
import io.pillopl.user.UserrightName;
import io.pillopl.util.ApiExceptionTranslator;
import io.pillopl.util.DateService;
import io.pillopl.util.SiteScopeHelper;
import io.pillopl.value.Username;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Monitoring and service calls of the console on top of {@link PedestalService}: reads need PEDESTAL_VIEW, commands
 * PEDESTAL_EDIT, the network-wide failed events CONFIG_EDIT; pedestals and sockets outside the caller's SITE scope
 * are reported as unknown.
 */
@Service
@Transactional
public class MonitoringApiService implements MonitoringApi {

    static final int DEFAULT_ROWS = 200;

    static final int MAX_ROWS = 1000;

    static final int DEFAULT_DAYS = 7;

    static final String STATUS_REQUEST = "STATUS_REQUEST";

    static final String FAULT_RESET = "FAULT_RESET";

    static final String FAULT_MAIL = "FAULT_MAIL";

    private final PedestalService pedestalService;

    private final SocketStatusLogDao socketStatusLogDao;

    private final GroupService groupService;

    private final DateService dateService;

    private final PrincipalResolver principalResolver;

    public MonitoringApiService(PedestalService pedestalService, SocketStatusLogDao socketStatusLogDao,
                                GroupService groupService, DateService dateService,
                                PrincipalResolver principalResolver) {
        this.pedestalService = pedestalService;
        this.socketStatusLogDao = socketStatusLogDao;
        this.groupService = groupService;
        this.dateService = dateService;
        this.principalResolver = principalResolver;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatusReportLineDTO> getStatusReport(Username authUsername, LocalDate month, Long campsiteGroupId) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.PEDESTAL_VIEW);
            List<Long> groupIds = scopeGroupIds(principal, campsiteGroupId);
            if (groupIds.isEmpty()) {
                return List.of();
            }
            LocalDate reportMonth = month != null ? month : dateService.getToday();
            Map<Long, String> pedestalCodes = new HashMap<>();
            List<StatusReportLineDTO> lines = new ArrayList<>();
            for (SocketStatusLogDao.StatusReportRow row : socketStatusLogDao.findStatusReport(reportMonth, groupIds)) {
                String pedestalCode = pedestalCodes.computeIfAbsent(row.getPedestalId(), id -> {
                    Pedestal pedestal = pedestalService.find(id);
                    return pedestal != null ? pedestal.getCode() : null;
                });
                lines.add(new StatusReportLineDTO(row.getSocketId(), row.getSocketName(), row.getPedestalId(),
                        pedestalCode, row.getGroupId(), row.getStatus(),
                        row.getTimeInSeconds() != null ? row.getTimeInSeconds() : 0L));
            }
            return lines;
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SocketStatusLogDTO> findSocketStatusLogs(Username authUsername, String pedestalCode, Long socketId,
                                                         DateRange period, Integer maxRows) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.PEDESTAL_VIEW);
            Socket socket = null;
            Pedestal pedestal;
            if (socketId != null) {
                socket = socketInScope(principal, socketId);
                pedestal = socket.getPedestal();
            } else {
                pedestal = pedestalInScope(principal, pedestalCode);
            }
            DateRange p = periodOrLastDays(period);
            List<SocketStatusLogDTO> logs = new ArrayList<>();
            for (SocketStatusLog log : pedestalService.findSocketStatusLogs(pedestal, socket, p.from(), p.to(),
                    rows(maxRows))) {
                logs.add(MonitoringMapper.toDTO(log, pedestalService.findErrorNames(pedestal, log.getErrorFlags())));
            }
            return logs;
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommunicationLogDTO> findCommunicationLogs(Username authUsername, String pedestalCode,
                                                           DateRange period, Integer maxRows) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.PEDESTAL_VIEW);
            Pedestal pedestal = pedestalInScope(principal, pedestalCode);
            DateRange p = periodOrLastDays(period);
            return pedestalService.findCommunicationLogs(pedestal, p.from(), p.to(), rows(maxRows)).stream()
                    .map(MonitoringMapper::toDTO).toList();
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<HeartbeatTimeoutDTO> findHeartbeatTimeouts(Username authUsername, Long campsiteGroupId) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.PEDESTAL_VIEW);
            List<HeartbeatTimeoutDTO> timeouts = new ArrayList<>();
            for (PedestalState state : pedestalService.findHeartbeatTimeouts(scopeGroupIds(principal,
                    campsiteGroupId))) {
                timeouts.add(MonitoringMapper.toDTO(state));
            }
            return timeouts;
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<FailedEventDTO> findFailedEvents(Username authUsername, Integer maxRows) {
        try {
            principalResolver.resolve(authUsername, UserrightName.CONFIG_EDIT);
            List<FailedEventDTO> events = new ArrayList<>();
            for (EventRetry retry : pedestalService.findFailedEvents(rows(maxRows))) {
                events.add(MonitoringMapper.toDTO(retry));
            }
            return events;
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public PedestalCommandDTO requestStatus(Username authUsername, String pedestalCode, Integer socketNo) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.PEDESTAL_EDIT);
            Pedestal pedestal = pedestalInScope(principal, pedestalCode);
            Socket socket = null;
            if (socketNo != null) {
                socket = pedestalService.findSocket(pedestal.getCode(), socketNo);
                if (socket == null || socket.isDeleted()) {
                    throw ObjectNotFoundException.of("Socket", pedestal.getCode() + "/" + socketNo);
                }
            }
            pedestalService.requestStatus(pedestal, socketNo);
            String message = "Status requested; the pedestal reports with its socket status";
            return socket != null ? SupplySessionMapper.toCommand(STATUS_REQUEST, socket, message)
                    : new PedestalCommandDTO(STATUS_REQUEST, pedestal.getCode(), null, null, true, null, message);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public PedestalCommandDTO resetFault(Username authUsername, Long socketId) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.PEDESTAL_EDIT);
            Socket socket = socketInScope(principal, socketId);
            String fault = socket.getStatus().name();
            boolean remote = pedestalService.resetFault(socket);
            return SupplySessionMapper.toCommand(FAULT_RESET, socket, remote
                    ? "Breaker of the socket switched on remotely (" + fault + ")"
                    : "Fault " + fault + " ended by " + principal.getUsername() + " (repaired on site)");
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PedestalCommandDTO resendFaultMail(Username authUsername, Long socketId) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.PEDESTAL_EDIT);
            Socket socket = socketInScope(principal, socketId);
            long errorFlags = pedestalService.resendFaultMail(socket);
            return SupplySessionMapper.toCommand(FAULT_MAIL, socket, "Fault mail sent again (error flags "
                    + errorFlags + ")");
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    private List<Long> scopeGroupIds(Userinfo principal, Long campsiteGroupId) {
        Long siteGroupId = SiteScopeHelper.siteGroupIdOf(principal, campsiteGroupId, groupService);
        if (siteGroupId == null) {
            return List.of();
        }
        io.pillopl.group.Group root = groupService.fetchByGroupId(siteGroupId);
        return root != null ? groupService.findManageable(root) : List.of();
    }

    private Pedestal pedestalInScope(Userinfo principal, String pedestalCode) {
        if (pedestalCode == null || pedestalCode.isBlank()) {
            throw new InvalidInputException("Pedestal code is missing");
        }
        Pedestal pedestal = pedestalService.findByCodeForUser(principal, pedestalCode.trim());
        if (pedestal == null || pedestal.isDeleted()) {
            throw ObjectNotFoundException.of("Pedestal", pedestalCode);
        }
        return pedestal;
    }

    private Socket socketInScope(Userinfo principal, Long socketId) {
        if (socketId == null) {
            throw new InvalidInputException("Socket id is missing");
        }
        Socket socket = pedestalService.findSocket(socketId);
        if (socket == null || socket.isDeleted() || !pedestalService.isManageable(principal, socket.getPedestal())) {
            throw ObjectNotFoundException.of("Socket", socketId);
        }
        return socket;
    }

    private DateRange periodOrLastDays(DateRange period) {
        LocalDateTime now = dateService.getNow();
        LocalDateTime from = period != null && period.from() != null ? period.from() : now.minusDays(DEFAULT_DAYS);
        LocalDateTime to = period != null && period.to() != null ? period.to() : now;
        return new DateRange(from, to);
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
