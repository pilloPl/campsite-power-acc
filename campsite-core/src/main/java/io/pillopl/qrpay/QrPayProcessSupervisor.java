package io.pillopl.qrpay;

import io.pillopl.api.QrPayApi;
import io.pillopl.api.QrPayConsoleApi;
import io.pillopl.api.exception.AccessNotAllowedException;
import io.pillopl.api.exception.ConflictException;
import io.pillopl.api.exception.InvalidInputException;
import io.pillopl.api.exception.ObjectNotFoundException;
import io.pillopl.card.Authorization;
import io.pillopl.card.AuthorizationResult;
import io.pillopl.card.ValidationStatus;
import io.pillopl.commons.DateRange;
import io.pillopl.commons.ListPage;
import io.pillopl.commons.Pagination;
import io.pillopl.commons.TextUtil;
import io.pillopl.config.SystemPropertyService;
import io.pillopl.dto.qrpay.QrPayProcessDTO;
import io.pillopl.dto.qrpay.QrPayProcessOverviewDTO;
import io.pillopl.dto.qrpay.QrPayTimelineDTO;
import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.GroupType;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.PedestalService;
import io.pillopl.infrastructure.Socket;
import io.pillopl.persistence.dao.QrPayProcessDao;
import io.pillopl.persistence.dao.QrPayProcessDaoCustom.QrPayChainRow;
import io.pillopl.pricing.PricingService;
import io.pillopl.qrpay.state.QrPayState;
import io.pillopl.qrpay.state.QrPayStateFactory;
import io.pillopl.supply.SupplySession;
import io.pillopl.supply.SupplySessionManager;
import io.pillopl.user.PrincipalResolver;
import io.pillopl.user.Userinfo;
import io.pillopl.user.UserrightName;
import io.pillopl.util.ApiExceptionTranslator;
import io.pillopl.util.DateService;
import io.pillopl.value.QrPayAuthTag;
import io.pillopl.value.SocketCode;
import io.pillopl.value.SocketLinkId;
import io.pillopl.value.Username;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Drives the QR payment processes: the app's start, stop and queries (with the owner check on every call for an
 * existing payment) and the pedestal's start and stop of the supply (from the supply listeners). Deliberately not
 * transactional: every transition commits on its own, so the pedestal finds the process when it answers.
 */
@Service
public class QrPayProcessSupervisor implements QrPayApi, QrPayConsoleApi {

    private static final Logger LOG = LoggerFactory.getLogger(QrPayProcessSupervisor.class);

    static final int MAX_SIGNAL_ATTEMPTS = 6;

    private final PrincipalResolver principalResolver;

    private final QrPayStateFactory stateFactory;

    private final QrPayProcessService qrPayProcessService;

    private final QrPayCostService qrPayCostService;

    private final PedestalService pedestalService;

    private final PricingService pricingService;

    private final Authorization authorization;

    private final SupplySessionManager supplySessionManager;

    private final DateService dateService;

    private final QrPayProcessDao qrPayProcessDao;

    private final GroupService groupService;

    private final SystemPropertyService systemPropertyService;

    private long signalRetryPauseInMillis = 200;

    public QrPayProcessSupervisor(PrincipalResolver principalResolver, QrPayStateFactory stateFactory,
                                  QrPayProcessService qrPayProcessService, QrPayCostService qrPayCostService,
                                  PedestalService pedestalService, PricingService pricingService,
                                  Authorization authorization, SupplySessionManager supplySessionManager,
                                  DateService dateService, QrPayProcessDao qrPayProcessDao, GroupService groupService,
                                  SystemPropertyService systemPropertyService) {
        this.qrPayProcessDao = qrPayProcessDao;
        this.groupService = groupService;
        this.systemPropertyService = systemPropertyService;
        this.principalResolver = principalResolver;
        this.stateFactory = stateFactory;
        this.qrPayProcessService = qrPayProcessService;
        this.qrPayCostService = qrPayCostService;
        this.pedestalService = pedestalService;
        this.pricingService = pricingService;
        this.authorization = authorization;
        this.supplySessionManager = supplySessionManager;
        this.dateService = dateService;
    }

    void setSignalRetryPauseInMillis(long signalRetryPauseInMillis) {
        this.signalRetryPauseInMillis = signalRetryPauseInMillis;
    }

    // ---------------------------------------------------------------- calls of the app

    @Override
    public QrPayProcessDTO startQrPay(Username authUsername, String orderReference, String socketCode,
                                      String appUserId) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.QRPAY_APP);
            String reference = required(orderReference, "Order reference");
            String appUser = required(appUserId, "App user");
            if (qrPayProcessService.findCurrentByOrderReference(reference) != null) {
                throw new ConflictException("Order reference " + reference + " already exists");
            }
            Socket socket = pedestalService.findSocketByCode(SocketCode.parse(socketCode).value());
            Pedestal pedestal = socket != null ? pedestalService.findBySocketId(socket.getId()) : null;
            if (socket == null || socket.isDeleted() || pedestal == null || pedestal.isDeleted()) {
                throw ObjectNotFoundException.of("Socket", socketCode);
            }
            final LocalDateTime now = dateService.getNow();
            // the price the guest agrees to: the tariff of the socket without card
            String priceInformation = pricingService.getPriceInformation(socket.getId(), null, now);

            QrPayState state = stateFactory.startProcess(reference, socket.getId(), principal.getUsername(),
                    appUser, now, priceInformation);
            Long processId = state.getProcess().getProcessId();
            LOG.info(String.format("QR payment %s for order %s at socket %s requested by %s/%s", processId,
                    reference, socket.getSocketCode(), principal.getUsername(), appUser));

            if (priceInformation == null) {
                state = state.handleError(QrPayErrorCode.NO_TARIFF, "No tariff for socket " + socket.getSocketCode());
                return toDTO(state.getProcess());
            }
            if (qrPayProcessService.hasOtherOpenProcess(socket.getId(), processId)) {
                state = state.handleError(QrPayErrorCode.SOCKET_UNAVAILABLE, "Another QR payment is open at socket "
                        + socket.getSocketCode());
                return toDTO(state.getProcess());
            }
            // the same checks as for a card at the socket: occupied, section power budget
            AuthorizationResult authorizationResult = authorization.authenticate(
                    QrPayAuthTag.of(processId).value(), SocketLinkId.of(pedestal.getCode(), socket.getSocketNo()));
            if (authorizationResult.getStatus() != ValidationStatus.ACCEPTED) {
                state = state.handleError(QrPayErrorCode.SOCKET_UNAVAILABLE, "Socket " + socket.getSocketCode()
                        + ": " + authorizationResult.getStatus());
                return toDTO(state.getProcess());
            }
            state = state.remoteSupplyStart();
            return toDTO(state.getProcess());
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public QrPayProcessDTO stopQrPay(Username authUsername, String orderReference, String appUserId) {
        try {
            QrPayProcess current = findOwnProcess(authUsername, orderReference, appUserId);
            QrPayState state = stateFactory.getState(current.detachedCopy());
            state.stopRequested();
            // second look at the chain: a stop the pedestal never got stays visible as STOP_REQUESTED or ERROR
            state = stateFactory.createState(current.getProcessId());
            state = state.remoteSupplyStop();
            return toDTO(state.getProcess());
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public QrPayProcessDTO getQrPayProcess(Username authUsername, String orderReference, String appUserId) {
        try {
            return toDTO(findOwnProcess(authUsername, orderReference, appUserId));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    /**
     * The payment of the order, if the caller started it for this app user (the source let any caller stop any
     * process).
     */
    QrPayProcess findOwnProcess(Username authUsername, String orderReference, String appUserId) {
        Userinfo principal = principalResolver.resolve(authUsername, UserrightName.QRPAY_APP);
        String reference = required(orderReference, "Order reference");
        QrPayProcess current = qrPayProcessService.findCurrentByOrderReference(reference);
        if (current == null) {
            throw ObjectNotFoundException.of("QR payment", reference);
        }
        if (!current.isOwnedBy(principal.getUsername(), appUserId)) {
            LOG.warn(String.format("%s/%s is not the owner of QR payment %s", principal.getUsername(), appUserId,
                    reference));
            throw new AccessNotAllowedException("QR payment " + reference + " belongs to another user");
        }
        return current;
    }

    // ---------------------------------------------------------------- calls of the console

    @Override
    public ListPage<QrPayProcessOverviewDTO> findQrPayProcesses(Username authUsername, String state,
                                                                Long campsiteGroupId, DateRange period,
                                                                Pagination pagination) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.REPORT_VIEW);
            Group siteGroup = principal.getGroup(GroupType.SITE);
            if (siteGroup == null) {
                return ListPage.empty();
            }
            QrPayStateName stateName = parseStateOrNull(state);
            DateRange range = period != null ? period : DateRange.UNBOUNDED;
            return qrPayProcessDao.findProcessOverviewPage(siteGroup.getId(), campsiteGroupId,
                    stateName != null ? stateName.name() : null, range.from(), range.to(), stuckBefore(),
                    pagination != null ? pagination : Pagination.FIRST_PAGE);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public QrPayTimelineDTO getQrPayTimeline(Username authUsername, Long processId) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.REPORT_VIEW);
            return timeline(findChainInScope(principal, processId));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    /**
     * The cancel goes through the state classes like the signals of the app and the pedestal: a supply that may run
     * is stopped (the price follows the stop of the pedestal), a pending price is taken now or the process fails
     * with CALCULATION_FAILED, a process without supply goes to ERROR (CANCELLED).
     */
    @Override
    public QrPayTimelineDTO cancelQrPayProcess(Username authUsername, Long processId) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.BILLING_EDIT);
            List<QrPayChainRow> chain = findChainInScope(principal, processId);
            QrPayChainRow last = chain.get(chain.size() - 1);
            if (last.archived()) {
                throw new ConflictException("QR payment " + processId + " is archived");
            }
            QrPayProcess current = qrPayProcessService.findCurrent(processId);
            String refusal = cancelRefusal(current, supplyRunning(current), stuckBefore());
            if (refusal != null) {
                throw new ConflictException("QR payment " + processId + " cannot be cancelled: " + refusal);
            }
            String message = "Cancelled by " + principal.getUsername();
            LOG.info(String.format("QR payment %s in %s cancelled by %s", processId, current.getStateName(),
                    principal.getUsername()));
            QrPayState state = stateFactory.getState(current.detachedCopy());
            switch (current.getStateName()) {
                case START_REQUESTED, SOCKET_START_WAITING, SOCKET_STOP_WAITING ->
                        state.handleError(QrPayErrorCode.CANCELLED, message);
                case SOCKET_START_RECEIVED, START_CONFIRMED, ERROR -> {
                    state.stopRequested();
                    stateFactory.createState(processId).remoteSupplyStop();
                }
                case STOP_REQUESTED -> state.remoteSupplyStop();
                case SOCKET_STOP_RECEIVED -> takePriceOrFail(state.requestCalculation(), message);
                case CALCULATION_REQUESTED -> takePriceOrFail(state, message);
                case CALCULATION_DONE -> state.close();
                default -> throw new ConflictException("QR payment " + processId + " is finished");
            }
            return timeline(qrPayProcessDao.findChain(processId));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public int runQrPayArchive(Username authUsername) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.BILLING_EDIT);
            int archived = qrPayProcessService.archiveFinishedProcesses();
            LOG.info(String.format("QR payment archive run by %s: %d rows", principal.getUsername(), archived));
            return archived;
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    /**
     * Why a process cannot be cancelled, null when it can: CLOSED is final, ERROR only while its supply still runs,
     * every other state once it is stuck (in the state since before {@code stuckBefore}).
     */
    static String cancelRefusal(QrPayProcess current, boolean supplyRunning, LocalDateTime stuckBefore) {
        QrPayStateName stateName = current.getStateName();
        if (stateName == QrPayStateName.CLOSED) {
            return "it is closed";
        }
        if (stateName == QrPayStateName.ERROR) {
            return supplyRunning ? null : "it failed and no supply runs";
        }
        if (current.getStartTime() != null && !current.getStartTime().isBefore(stuckBefore)) {
            return "it is not stuck, its state began at " + current.getStartTime();
        }
        return null;
    }

    private void takePriceOrFail(QrPayState state, String message) {
        QrPayState priced = state.calculate();
        if (priced.getStateName() == QrPayStateName.CALCULATION_REQUESTED) {
            priced.handleError(QrPayErrorCode.CALCULATION_FAILED, message + ": no consumption record for session "
                    + priced.getProcess().getSupplySessionId());
        } else if (priced.getStateName() == QrPayStateName.CALCULATION_DONE) {
            priced.close();
        }
    }

    private boolean supplyRunning(QrPayProcess process) {
        if (process.getTransactionId() == null) {
            return false;
        }
        SupplySession session = supplySessionManager.getSession(process.getSupplySessionId());
        return session != null && session.isActive();
    }

    private LocalDateTime stuckBefore() {
        return dateService.getNow().minusMinutes(systemPropertyService.getQrPayStuckAfterMinutes());
    }

    /**
     * @throws ObjectNotFoundException when the process is unknown or its socket outside the SITE scope of the user
     */
    private List<QrPayChainRow> findChainInScope(Userinfo principal, Long processId) {
        if (processId == null) {
            throw new InvalidInputException("Process id is missing");
        }
        List<QrPayChainRow> chain = qrPayProcessDao.findChain(processId);
        if (chain.isEmpty()) {
            throw ObjectNotFoundException.of("QR payment", processId);
        }
        Pedestal pedestal = pedestalService.findBySocketId(chain.get(0).socketId());
        Group siteGroup = principal.getGroup(GroupType.SITE);
        if (pedestal == null || siteGroup == null || !groupService.isManageable(siteGroup, pedestal.getGroup())) {
            LOG.info(String.format("QR payment %s is outside the scope of %s", processId, principal.getUsername()));
            throw ObjectNotFoundException.of("QR payment", processId);
        }
        return chain;
    }

    QrPayTimelineDTO timeline(List<QrPayChainRow> chain) {
        QrPayChainRow last = chain.get(chain.size() - 1);
        List<QrPayTimelineDTO.Step> steps = new ArrayList<>();
        for (QrPayChainRow row : chain) {
            steps.add(new QrPayTimelineDTO.Step(row.id(), row.state(), row.startTime(), row.stopTime(),
                    row.transactionId(), row.errorCode(), row.errorMessage()));
        }
        boolean cancellable = false;
        if (!last.archived()) {
            QrPayProcess current = qrPayProcessService.findCurrent(last.processId());
            cancellable = current != null && cancelRefusal(current, supplyRunning(current), stuckBefore()) == null;
        }
        return new QrPayTimelineDTO(last.processId(), last.orderReference(), last.state(), last.archived(),
                cancellable, steps, stateNames(), transitions());
    }

    static List<String> stateNames() {
        List<String> names = new ArrayList<>();
        for (QrPayStateName stateName : QrPayStateName.values()) {
            names.add(stateName.name());
        }
        return names;
    }

    static List<QrPayTimelineDTO.Transition> transitions() {
        List<QrPayTimelineDTO.Transition> transitions = new ArrayList<>();
        for (QrPayStateName from : QrPayStateName.values()) {
            for (QrPayStateName to : from.getNextStates()) {
                transitions.add(new QrPayTimelineDTO.Transition(from.name(), to.name()));
            }
        }
        return transitions;
    }

    private static QrPayStateName parseStateOrNull(String state) {
        String trimmed = TextUtil.trimToNull(state);
        if (trimmed == null) {
            return null;
        }
        try {
            return QrPayStateName.valueOf(trimmed.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidInputException("Unknown QR payment state: " + state, e);
        }
    }

    // ---------------------------------------------------------------- signals of the pedestal

    /**
     * The pedestal reported the start of the supply of the process: the start is recorded and confirmed to the app.
     */
    public void onSupplyStarted(Long processId, Long supplySessionId, Integer transactionId) {
        QrPayState state = awaitState(processId, "supplyStarted");
        if (state == null) {
            return;
        }
        state = state.supplyStarted(supplySessionId, transactionId);
        if (state.getStateName() == QrPayStateName.SOCKET_START_RECEIVED) {
            state.confirmStart();
        }
    }

    /**
     * The pedestal reported the stop of the supply: the price of the consumption record closes the payment.
     */
    public void onSupplyStopped(Long processId, Long supplySessionId, Integer transactionId) {
        QrPayState state = awaitState(processId, "supplyStopped");
        if (state == null) {
            return;
        }
        state = state.supplyStopped(supplySessionId, transactionId);
        if (state.getStateName() != QrPayStateName.SOCKET_STOP_RECEIVED) {
            return;
        }
        state = state.requestCalculation();
        // the record is written by the stop of the pedestal, maybe not committed yet
        for (int attempt = 1; state.getStateName() == QrPayStateName.CALCULATION_REQUESTED; attempt++) {
            state = state.calculate();
            if (state.getStateName() == QrPayStateName.CALCULATION_REQUESTED) {
                if (attempt >= MAX_SIGNAL_ATTEMPTS) {
                    state = state.handleError(QrPayErrorCode.CALCULATION_FAILED, "No consumption record for session "
                            + supplySessionId);
                } else {
                    pause();
                }
            }
        }
        if (state.getStateName() == QrPayStateName.CALCULATION_DONE) {
            state = state.close();
            LOG.info(String.format("QR payment %s closed with %s %s", processId, state.getProcess().getCost(),
                    state.getProcess().getCurrency()));
        }
    }

    /**
     * The state for a signal of the pedestal; while the state waits for its own transition (remote command just
     * sent) the signal is retried a few times.
     */
    private QrPayState awaitState(Long processId, String signal) {
        for (int attempt = 1; ; attempt++) {
            QrPayState state = stateFactory.createState(processId);
            if (state == null) {
                LOG.warn(String.format("No QR payment %s for %s", processId, signal));
                return null;
            }
            if (!state.awaitsOwnTransition() || attempt >= MAX_SIGNAL_ATTEMPTS) {
                return state;
            }
            LOG.warn(String.format("Received %s on %s - retry %d of %d", signal, state, attempt,
                    MAX_SIGNAL_ATTEMPTS));
            pause();
        }
    }

    private void pause() {
        if (signalRetryPauseInMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(signalRetryPauseInMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ---------------------------------------------------------------- mapping

    QrPayProcessDTO toDTO(QrPayProcess process) {
        Socket socket = pedestalService.findSocket(process.getSocketId());
        SupplySession session = supplySessionManager.getSession(process.getSupplySessionId());
        QrPayCost cost = process.getCost() != null
                ? new QrPayCost(process.getKwh(), process.getDurationSeconds(), process.getCost(),
                process.getCurrency(), true, null)
                : process.getStateName().isFinished() ? QrPayCost.nothingYet()
                : qrPayCostService.calculatePreliminaryCost(process);
        return toDTO(process, socket != null ? socket.getSocketCode() : null, session, cost);
    }

    static QrPayProcessDTO toDTO(QrPayProcess process, String socketCode, SupplySession session, QrPayCost cost) {
        return new QrPayProcessDTO(process.getProcessId(), process.getOrderReference(), socketCode,
                process.getState(), process.getRequestDate(), process.getPriceInformation(),
                process.getTransactionId(), session != null ? session.getRemoteDate() : null,
                session != null ? session.getStopDate() : null, cost.kwh(), cost.durationSeconds(),
                cost.finished() ? cost.cost() : null, cost.finished() ? null : cost.cost(), cost.currency(),
                process.getErrorCode() != null ? process.getErrorCode().name() : null,
                process.getErrorCode() != null ? process.getErrorCode().getText() : null);
    }

    private static String required(String value, String what) {
        String trimmed = TextUtil.trimToNull(value);
        if (trimmed == null) {
            throw new InvalidInputException(what + " is missing");
        }
        return trimmed;
    }
}
