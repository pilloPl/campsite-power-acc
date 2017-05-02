package io.pillopl.supply;

import io.pillopl.api.SupplyApi;
import io.pillopl.api.exception.InvalidInputException;
import io.pillopl.api.exception.ObjectNotFoundException;
import io.pillopl.commons.DateRange;
import io.pillopl.commons.ListPage;
import io.pillopl.commons.Pagination;
import io.pillopl.config.SystemPropertyService;
import io.pillopl.dto.infrastructure.PedestalCommandDTO;
import io.pillopl.dto.supply.SupplyIncidentDTO;
import io.pillopl.dto.supply.SupplySessionDTO;
import io.pillopl.group.GroupService;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.PedestalService;
import io.pillopl.infrastructure.Socket;
import io.pillopl.persistence.dao.ConsumptionRecordDao;
import io.pillopl.persistence.dao.OperationsViewDao;
import io.pillopl.persistence.dao.SupplyIncidentDao;
import io.pillopl.user.PrincipalResolver;
import io.pillopl.user.Userinfo;
import io.pillopl.user.UserrightName;
import io.pillopl.util.ApiExceptionTranslator;
import io.pillopl.util.DateService;
import io.pillopl.util.SiteScopeHelper;
import io.pillopl.value.Username;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Supply session calls of the console on top of {@link SupplyControlUnitImpl} and the view SQL. Lists need
 * PEDESTAL_VIEW, the remote control SOCKET_REMOTE_CONTROL, the incident review PEDESTAL_EDIT; sockets, sessions
 * and incidents outside the caller's SITE scope are reported as unknown.
 */
@Service
@Transactional
public class SupplyApiService implements SupplyApi {

    static final String SUPPLY_START = "SUPPLY_START";

    static final String SUPPLY_STOP = "SUPPLY_STOP";

    private final SupplyControlUnitImpl supplyControlUnit;

    private final SupplySessionManager sessionManager;

    private final PedestalService pedestalService;

    private final OperationsViewDao operationsViewDao;

    private final SupplyIncidentDao supplyIncidentDao;

    private final ConsumptionRecordDao consumptionRecordDao;

    private final GroupService groupService;

    private final SystemPropertyService systemPropertyService;

    private final DateService dateService;

    private final PrincipalResolver principalResolver;

    public SupplyApiService(SupplyControlUnitImpl supplyControlUnit, SupplySessionManager sessionManager,
                            PedestalService pedestalService, OperationsViewDao operationsViewDao,
                            SupplyIncidentDao supplyIncidentDao, ConsumptionRecordDao consumptionRecordDao,
                            GroupService groupService, SystemPropertyService systemPropertyService,
                            DateService dateService, PrincipalResolver principalResolver) {
        this.supplyControlUnit = supplyControlUnit;
        this.sessionManager = sessionManager;
        this.pedestalService = pedestalService;
        this.operationsViewDao = operationsViewDao;
        this.supplyIncidentDao = supplyIncidentDao;
        this.consumptionRecordDao = consumptionRecordDao;
        this.groupService = groupService;
        this.systemPropertyService = systemPropertyService;
        this.dateService = dateService;
        this.principalResolver = principalResolver;
    }

    @Override
    @Transactional(readOnly = true)
    public ListPage<SupplySessionDTO> findSupplySessions(Username authUsername, Long campsiteGroupId,
                                                         String pedestalCode, String socketCode, boolean activeOnly,
                                                         DateRange period, Pagination pagination) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.PEDESTAL_VIEW);
            Long siteGroupId = SiteScopeHelper.siteGroupIdOf(principal, campsiteGroupId, groupService);
            if (siteGroupId == null) {
                return ListPage.of(java.util.List.of(), 0, pagination);
            }
            DateRange p = orUnbounded(period);
            return operationsViewDao.findSupplySessions(siteGroupId, pedestalCode, socketCode, activeOnly, p.from(),
                    p.to(), silentSince(), pagination);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    /**
     * A list without a period is not limited in time.
     */
    private static DateRange orUnbounded(DateRange period) {
        return period != null ? period : DateRange.UNBOUNDED;
    }

    @Override
    public PedestalCommandDTO startSupply(Username authUsername, Long socketId, String cardRfid) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.SOCKET_REMOTE_CONTROL);
            if (cardRfid == null || cardRfid.isBlank()) {
                throw new InvalidInputException("The card to supply for is missing");
            }
            Socket socket = socketInScope(principal, socketId);
            supplyControlUnit.requestSupplyStart(socket, cardRfid.trim());
            return SupplySessionMapper.toCommand(SUPPLY_START, socket, "Supply start requested; the session "
                    + "starts with the pedestal's report");
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public PedestalCommandDTO stopSupply(Username authUsername, Long sessionId) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.SOCKET_REMOTE_CONTROL);
            SupplySession session = sessionInScope(principal, sessionId);
            supplyControlUnit.requestSupplyStop(session);
            return SupplySessionMapper.toCommand(SUPPLY_STOP, session.getSocket(), "Supply stop of transaction "
                    + session.getTransactionId() + " requested; the session ends with the pedestal's report");
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public SupplySessionDTO closeStaleSession(Username authUsername, Long sessionId) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.SOCKET_REMOTE_CONTROL);
            SupplySession closed = supplyControlUnit.closeStaleSession(sessionInScope(principal, sessionId));
            return SupplySessionMapper.toDTO(closed, consumptionRecordDao.findBySupplySessionId(closed.getId()),
                    false);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ListPage<SupplyIncidentDTO> findSupplyIncidents(Username authUsername, Long campsiteGroupId,
                                                           String pedestalCode, boolean unreviewedOnly,
                                                           DateRange period, Pagination pagination) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.PEDESTAL_VIEW);
            Long siteGroupId = SiteScopeHelper.siteGroupIdOf(principal, campsiteGroupId, groupService);
            if (siteGroupId == null) {
                return ListPage.of(java.util.List.of(), 0, pagination);
            }
            DateRange p = orUnbounded(period);
            return operationsViewDao.findSupplyIncidents(siteGroupId, pedestalCode, unreviewedOnly, p.from(), p.to(),
                    pagination);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public SupplyIncidentDTO reviewSupplyIncident(Username authUsername, Long incidentId, String comment) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.PEDESTAL_EDIT);
            if (incidentId == null) {
                throw new InvalidInputException("Supply incident id is missing");
            }
            SupplyIncident incident = supplyIncidentDao.findById(incidentId).orElse(null);
            Pedestal pedestal = incident != null ? pedestalService.findByCode(incident.getPedestalCode()) : null;
            if (pedestal == null || !pedestalService.isManageable(principal, pedestal)) {
                throw ObjectNotFoundException.of("Supply incident", incidentId);
            }
            SupplyIncident reviewed = supplyControlUnit.reviewIncident(incidentId, principal.getUsername(), comment);
            return SupplySessionMapper.toDTO(reviewed, pedestal.getGroup() != null ? pedestal.getGroup().getId()
                    : null);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    private Socket socketInScope(Userinfo principal, Long socketId) {
        Socket socket = socketId != null ? pedestalService.findSocket(socketId) : null;
        if (socket == null || socket.isDeleted() || !pedestalService.isManageable(principal, socket.getPedestal())) {
            throw ObjectNotFoundException.of("Socket", socketId);
        }
        return socket;
    }

    private SupplySession sessionInScope(Userinfo principal, Long sessionId) {
        if (sessionId == null) {
            throw new InvalidInputException("Supply session id is missing");
        }
        SupplySession session = sessionManager.getSession(sessionId);
        if (session == null || !pedestalService.isManageable(principal, session.getSocket().getPedestal())) {
            throw ObjectNotFoundException.of("Supply session", sessionId);
        }
        return session;
    }

    private java.time.LocalDateTime silentSince() {
        return dateService.getNow().minusHours(systemPropertyService.getStaleSessionTimeoutInHours());
    }
}
