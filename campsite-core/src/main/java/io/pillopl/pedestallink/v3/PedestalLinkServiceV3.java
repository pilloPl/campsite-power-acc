package io.pillopl.pedestallink.v3;

import io.pillopl.card.Authorization;
import io.pillopl.card.ValidationStatus;
import io.pillopl.config.SystemPropertyService;
import io.pillopl.dto.pedestallink.v1.AcknowledgeResponse;
import io.pillopl.event.EventRetryService;
import io.pillopl.group.attribute.NotificationType;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.PedestalService;
import io.pillopl.infrastructure.Socket;
import io.pillopl.infrastructure.SocketStatus;
import io.pillopl.monitoring.PedestalMailService;
import io.pillopl.monitoring.SocketStateTaskWorker;
import io.pillopl.pedestallink.v2.PedestalLinkClientV2;
import io.pillopl.persistence.dao.SocketStatusLogDao;
import io.pillopl.supply.SupplyControlUnitImpl;
import io.pillopl.supply.SupplySessionManager;
import io.pillopl.util.DateService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Third generation of the pedestal link: every message has its handler ({@link PedestalLinkV3Handler}), registered
 * here at start-up and called by its class. The service itself only locates: the handlers reach the collaborators
 * through its getters. Messages and answers are the v1 and v2 bodies.
 */
@Service
public class PedestalLinkServiceV3 {

    private static final Logger LOGGER = LoggerFactory.getLogger(PedestalLinkServiceV3.class);

    /**
     * Translation between the link v3 texts and the pedestal backend.
     */
    public abstract static class TranslationHelper {

        static final String ACCEPTED = "ACCEPTED";

        static final String REJECTED = "REJECTED";

        static final String BLOCKED = "BLOCKED";

        static final String EXPIRED = "EXPIRED";

        static final String INVALID = "INVALID";

        static final String OCCUPIED = "OCCUPIED";

        static final String UNAVAILABLE = "UNAVAILABLE";

        static final String NOT_ACCEPTED_HERE = "NOT_ACCEPTED_HERE";

        static final String BREAKER_TRIPPED = "BREAKER_TRIPPED";

        public static String toLinkStatus(final ValidationStatus status) {
            if (status == null) {
                return INVALID;
            }

            switch (status) {
                case ACCEPTED:
                    return ACCEPTED;
                case SOCKET_OCCUPIED:
                    return OCCUPIED;
                case EXPIRED:
                    return EXPIRED;
                case BLOCKED:
                    return BLOCKED;
                case UNAVAILABLE:
                    return UNAVAILABLE;
                case CARD_NOT_ACCEPTED_AT_SITE:
                    return NOT_ACCEPTED_HERE;
                default:
                    return INVALID;
            }
        }

        public static SocketStatus translateSocketStatus(final String status, final String errorCode) {
            if (status == null) {
                throw new IllegalArgumentException("Link v3 socket status is missing");
            }
            switch (status) {
                case "AVAILABLE":
                    return SocketStatus.AVAILABLE;
                case "OCCUPIED":
                    return SocketStatus.OCCUPIED;
                case "SUPPLYING":
                    return SocketStatus.SUPPLYING;
                case "BREAKER_TRIPPED":
                    return SocketStatus.BREAKER_TRIPPED;
                case "FAULTED":
                    return BREAKER_TRIPPED.equals(errorCode) ? SocketStatus.BREAKER_TRIPPED : SocketStatus.FAULTED;
                case "UNAVAILABLE":
                    return SocketStatus.UNAVAILABLE;
                case "FIRMWARE_UPDATE":
                    return SocketStatus.FIRMWARE_UPDATE;
                default:
                    throw new IllegalArgumentException("Unknown link v3 socket status " + status);
            }
        }

        /**
         * A socket in error first becomes available (repaired, breaker reset) before it supplies again; a supply
         * starts from a plugged-in socket. Anything else is possible.
         */
        public static boolean isExpectedTransition(final SocketStatus previous, final SocketStatus next) {
            if (previous == null || next == null || previous == next) {
                return true;
            }
            if (previous.isError() && (next == SocketStatus.SUPPLYING || next == SocketStatus.OCCUPIED)) {
                return false;
            }
            return next != SocketStatus.SUPPLYING || previous != SocketStatus.UNAVAILABLE;
        }

        /**
         * Link v3 sends meter values as whole Wh.
         */
        public static BigDecimal toKwh(final Integer valueWh) {
            return valueWh != null ? BigDecimal.valueOf(valueWh).movePointLeft(3) : null;
        }

        public static String decodeAuthTag(final String authTag) {
            return authTag != null ? authTag.trim() : null;
        }
    }

    private final Map<Class<?>, PedestalLinkV3Handler<?, ?>> handlers = new ConcurrentHashMap<>();

    private final Authorization authorization;

    private final DateService dateService;

    private final SystemPropertyService systemPropertyService;

    private final SupplyControlUnitImpl supplyControlUnit;

    private final SupplySessionManager supplySessionManager;

    private final PedestalService pedestalService;

    private final EventRetryService eventRetryService;

    private final PedestalLinkClientV2 pedestalLinkClient;

    private final SocketStateTaskWorker socketStateTaskWorker;

    private final SocketStatusLogDao socketStatusLogDao;

    private final PedestalMailService pedestalMailService;

    public PedestalLinkServiceV3(Authorization authorization, DateService dateService,
                                 SystemPropertyService systemPropertyService, SupplyControlUnitImpl supplyControlUnit,
                                 SupplySessionManager supplySessionManager, PedestalService pedestalService,
                                 EventRetryService eventRetryService, PedestalLinkClientV2 pedestalLinkClient,
                                 SocketStateTaskWorker socketStateTaskWorker, SocketStatusLogDao socketStatusLogDao,
                                 PedestalMailService pedestalMailService) {
        this.authorization = authorization;
        this.dateService = dateService;
        this.systemPropertyService = systemPropertyService;
        this.supplyControlUnit = supplyControlUnit;
        this.supplySessionManager = supplySessionManager;
        this.pedestalService = pedestalService;
        this.eventRetryService = eventRetryService;
        this.pedestalLinkClient = pedestalLinkClient;
        this.socketStateTaskWorker = socketStateTaskWorker;
        this.socketStatusLogDao = socketStatusLogDao;
        this.pedestalMailService = pedestalMailService;
    }

    @PostConstruct
    public void registerHandlers() {
        // register the handlers of link v3
        register(new PedestalLinkV3Handler.Authorize(this));
        register(new PedestalLinkV3Handler.Boot(this));
        register(new PedestalLinkV3Handler.Heartbeat(this));
        register(new PedestalLinkV3Handler.SupplyStarted(this));
        register(new PedestalLinkV3Handler.SupplyStopped(this));
        register(new PedestalLinkV3Handler.SocketStatusHandler(this));
        register(new PedestalLinkV3Handler.MeterReadings(this));
        register(new PedestalLinkV3Handler.FirmwareStatus(this));
        register(new PedestalLinkV3Handler.DiagnosticsStatus(this));
        register(new PedestalLinkV3Handler.VendorData(this));
        LOGGER.info(String.format("Registered %d link v3 handlers", handlers.size()));
    }

    void register(PedestalLinkV3Handler<?, ?> handler) {
        handlers.put(handler.getClass(), handler);
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public <REQ, RESP, T extends PedestalLinkV3Handler<REQ, RESP>> RESP handleRequest(final Class<T> handlerClass,
                                                                                     final String pedestalCode,
                                                                                     final REQ request) {
        if (handlerClass == null) {
            throw new IllegalArgumentException("Link v3 handler class must not be null");
        }

        final PedestalLinkV3Handler<REQ, RESP> handler = (PedestalLinkV3Handler<REQ, RESP>) handlers.get(handlerClass);
        if (handler == null) {
            throw new IllegalStateException(String.format("Unknown link v3 handler %s, not registered?",
                    handlerClass.getName()));
        }

        return handler.processRequest(request, pedestalCode);
    }

    // ---------------------------------------------------------------- helpers of the handlers

    public Pedestal getPedestal(final String pedestalCode) {
        Pedestal pedestal = pedestalCode != null ? pedestalService.findByCode(pedestalCode) : null;
        if (pedestal == null) {
            throw new IllegalArgumentException("Unknown pedestal " + pedestalCode);
        }
        return pedestal;
    }

    public Socket getSocket(final String pedestalCode, final Integer socketNo) {
        Socket socket = pedestalService.findSocket(pedestalCode, socketNo);
        if (socket == null) {
            throw new IllegalArgumentException(String.format("Socket %s of pedestal %s not found", socketNo,
                    pedestalCode));
        }
        return socket;
    }

    public void registerIncoming(final String pedestalCode) {
        Pedestal pedestal = pedestalService.findByCode(pedestalCode);
        if (pedestal != null) {
            pedestalService.registerIncoming(pedestal, dateService.getNow());
        }
    }

    public void sendQueuedLocalList(final Pedestal pedestal) {
        if (pedestal != null && pedestalLinkClient.hasQueuedLocalList(pedestal)) {
            try {
                pedestalLinkClient.sendQueuedLocalList(pedestal);
            } catch (Exception e) {
                LOGGER.error(String.format("Sending the queued local card list to %s failed", pedestal), e);
            }
        }
    }

    /**
     * Firmware and diagnostics status: only mailed to the service recipient of the pedestal, always acknowledged.
     */
    public AcknowledgeResponse mailStatus(final String pedestalCode, final NotificationType notificationType,
                                          final String status) {
        registerIncoming(pedestalCode);
        LocalDateTime now = dateService.getNow();
        try {
            pedestalMailService.sendStatusMail(getPedestal(pedestalCode), notificationType, status, now);
        } catch (Exception e) {
            LOGGER.error(String.format("%s %s of pedestal %s not mailed", notificationType, status, pedestalCode), e);
        }
        return new AcknowledgeResponse(now);
    }

    // ---------------------------------------------------------------- locator

    public Authorization getAuthorization() {
        return authorization;
    }

    public DateService getDateService() {
        return dateService;
    }

    public SystemPropertyService getSystemPropertyService() {
        return systemPropertyService;
    }

    public SupplyControlUnitImpl getSupplyControlUnit() {
        return supplyControlUnit;
    }

    public SupplySessionManager getSupplySessionManager() {
        return supplySessionManager;
    }

    public PedestalService getPedestalService() {
        return pedestalService;
    }

    public EventRetryService getEventRetryService() {
        return eventRetryService;
    }

    public PedestalLinkClientV2 getPedestalLinkClient() {
        return pedestalLinkClient;
    }

    public SocketStateTaskWorker getSocketStateTaskWorker() {
        return socketStateTaskWorker;
    }

    public SocketStatusLogDao getSocketStatusLogDao() {
        return socketStatusLogDao;
    }

    public PedestalMailService getPedestalMailService() {
        return pedestalMailService;
    }
}
