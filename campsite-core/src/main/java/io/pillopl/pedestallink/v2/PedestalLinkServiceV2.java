package io.pillopl.pedestallink.v2;

import io.pillopl.card.Authorization;
import io.pillopl.card.AuthorizationResult;
import io.pillopl.card.ValidationStatus;
import io.pillopl.config.SystemPropertyService;
import io.pillopl.dto.pedestallink.v1.AcknowledgeResponse;
import io.pillopl.dto.pedestallink.v1.AuthorizeRequest;
import io.pillopl.dto.pedestallink.v1.AuthorizeResponse;
import io.pillopl.dto.pedestallink.v1.BootRequest;
import io.pillopl.dto.pedestallink.v1.BootResponse;
import io.pillopl.dto.pedestallink.v1.DiagnosticsStatusRequest;
import io.pillopl.dto.pedestallink.v1.FirmwareStatusRequest;
import io.pillopl.dto.pedestallink.v1.MeterReading;
import io.pillopl.dto.pedestallink.v1.MeterReadingsRequest;
import io.pillopl.dto.pedestallink.v1.SupplyStartedRequest;
import io.pillopl.dto.pedestallink.v1.SupplyStartedResponse;
import io.pillopl.dto.pedestallink.v1.SupplyStoppedRequest;
import io.pillopl.dto.pedestallink.v1.SupplyStoppedResponse;
import io.pillopl.dto.pedestallink.v2.SocketStatusRequest;
import io.pillopl.dto.pedestallink.v2.VendorDataRequest;
import io.pillopl.dto.pedestallink.v2.VendorDataResponse;
import io.pillopl.event.Event;
import io.pillopl.event.EventAttribute;
import io.pillopl.event.EventRetryService;
import io.pillopl.event.EventService;
import io.pillopl.event.EventType;
import io.pillopl.group.attribute.NotificationType;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.PedestalService;
import io.pillopl.infrastructure.Socket;
import io.pillopl.infrastructure.SocketStatus;
import io.pillopl.monitoring.PedestalMailService;
import io.pillopl.monitoring.SocketStateParams;
import io.pillopl.monitoring.SocketStateTaskWorker;
import io.pillopl.monitoring.SocketStatusLog;
import io.pillopl.persistence.dao.SocketStatusLogDao;
import io.pillopl.supply.StopReason;
import io.pillopl.supply.SupplyControlUnitImpl;
import io.pillopl.supply.SupplyRequest;
import io.pillopl.supply.SupplyResponse;
import io.pillopl.supply.SupplySession;
import io.pillopl.supply.SupplySessionManager;
import io.pillopl.util.DateService;
import io.pillopl.value.SocketLinkId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Second generation of the pedestal link: a copy of the first one, one method per message of the controller, with the
 * v2 additions. The socket status knows more statuses and carries a vendor error code, which is translated with the
 * mask of the manufacturer before the mask of the link; the status event goes out through the event retry; the
 * controller can send vendor data. An authorization refused for the power budget is told as UNAVAILABLE.
 */
@Service
public class PedestalLinkServiceV2 {

    private static final Logger LOGGER = LoggerFactory.getLogger(PedestalLinkServiceV2.class);

    static final String ACCEPTED = "ACCEPTED";

    static final String REJECTED = "REJECTED";

    static final String BLOCKED = "BLOCKED";

    static final String EXPIRED = "EXPIRED";

    static final String INVALID = "INVALID";

    static final String OCCUPIED = "OCCUPIED";

    static final String UNAVAILABLE = "UNAVAILABLE";

    static final String BREAKER_TRIPPED = "BREAKER_TRIPPED";

    private static final String NOT_SUPPORTED = "";

    private final Authorization authorization;

    private final DateService dateService;

    private final SystemPropertyService systemPropertyService;

    private final SupplyControlUnitImpl supplyControlUnit;

    private final SupplySessionManager supplySessionManager;

    private final PedestalService pedestalService;

    private final EventService eventService;

    private final EventRetryService eventRetryService;

    private final PedestalLinkClientV2 pedestalLinkClient;

    private final SocketStateTaskWorker socketStateTaskWorker;

    private final SocketStatusLogDao socketStatusLogDao;

    private final PedestalMailService pedestalMailService;

    public PedestalLinkServiceV2(Authorization authorization, DateService dateService,
                                 SystemPropertyService systemPropertyService, SupplyControlUnitImpl supplyControlUnit,
                                 SupplySessionManager supplySessionManager, PedestalService pedestalService,
                                 EventService eventService, EventRetryService eventRetryService,
                                 PedestalLinkClientV2 pedestalLinkClient, SocketStateTaskWorker socketStateTaskWorker,
                                 SocketStatusLogDao socketStatusLogDao, PedestalMailService pedestalMailService) {
        this.authorization = authorization;
        this.dateService = dateService;
        this.systemPropertyService = systemPropertyService;
        this.supplyControlUnit = supplyControlUnit;
        this.supplySessionManager = supplySessionManager;
        this.pedestalService = pedestalService;
        this.eventService = eventService;
        this.eventRetryService = eventRetryService;
        this.pedestalLinkClient = pedestalLinkClient;
        this.socketStateTaskWorker = socketStateTaskWorker;
        this.socketStatusLogDao = socketStatusLogDao;
        this.pedestalMailService = pedestalMailService;
    }

    @Transactional
    public AuthorizeResponse authorize(String pedestalCode, AuthorizeRequest request) {
        registerIncoming(pedestalCode);

        String cardRfid = decodeAuthTag(request.authTag());

        AuthorizationResult authorizationResult = request.socketNo() != null
                ? authorization.authenticate(cardRfid, SocketLinkId.of(pedestalCode, request.socketNo()))
                : authorization.authenticate(cardRfid, pedestalCode);

        return new AuthorizeResponse(translateFrom(authorizationResult.getStatus()),
                authorizationResult.getExpiryDate(), authorizationResult.getGuestName());
    }

    @Transactional
    public BootResponse boot(String pedestalCode, BootRequest request) {

        Pedestal pedestal = pedestalService.updateBootData(pedestalCode, request.firmwareVersion(),
                request.serialNumber(), request.modemIccid());
        LocalDateTime now = dateService.getNow();
        if (pedestal != null) {
            pedestalService.registerIncoming(pedestal, now);
        }

        BootResponse bootResponse = new BootResponse(createRegistrationStatus(pedestal), now,
                systemPropertyService.getPedestalHeartbeatIntervalInSeconds());

        createBootEvent(pedestal, request, bootResponse);
        sendQueuedLocalList(pedestal);
        return bootResponse;
    }

    private static String createRegistrationStatus(final Pedestal pedestal) {
        String status = REJECTED;
        if (pedestal != null) {
            status = ACCEPTED;
        }
        return status;
    }

    private void createBootEvent(Pedestal pedestal, BootRequest request, BootResponse response) {
        if (pedestal != null) {
            try {
                Event event = new Event(EventType.BOOT_NOTIFICATION);
                event.setAttribute(EventAttribute.PEDESTAL_ID, pedestal.getId());
                event.setAttribute(EventAttribute.PEDESTAL_CODE, pedestal.getCode());
                event.setAttribute(EventAttribute.FIRMWARE_VERSION, request.firmwareVersion());
                event.setAttribute(EventAttribute.MODEM_ICCID, request.modemIccid());
                event.setAttribute(EventAttribute.METER_SERIAL_NUMBER, request.meterSerialNumber());
                event.setAttribute(EventAttribute.PEDESTAL_SERIAL_NUMBER, request.serialNumber());
                event.setAttribute(EventAttribute.PEDESTAL_VENDOR, request.vendor());
                event.setAttribute(EventAttribute.PEDESTAL_MODEL, request.model());
                event.setAttribute(EventAttribute.TIME, response.currentTime());
                event.setAttribute(EventAttribute.STATUS, response.status());
                event.setAttribute(EventAttribute.HEARTBEAT_INTERVAL, response.heartbeatInterval());
                LOGGER.debug(String.format("Created boot event %s", event));
                eventService.send(event);
            } catch (Exception e) {
                LOGGER.error("Error sending boot event.", e);
            }
        }
    }

    @Transactional
    public AcknowledgeResponse heartbeat(String pedestalCode) {

        LocalDateTime modDate = dateService.getNow();

        Pedestal pedestal = pedestalService.findByCode(pedestalCode);
        if (pedestal != null) {
            // the link state carries the heartbeat; the sockets keep the time of their last reading or status
            pedestalService.registerIncoming(pedestal, modDate);
            sendQueuedLocalList(pedestal);
        } else {
            LOGGER.warn(String.format("Heartbeat of unknown pedestal %s", pedestalCode));
        }

        return new AcknowledgeResponse(modDate);
    }

    @Transactional
    public SupplyStartedResponse supplyStarted(String pedestalCode, SupplyStartedRequest request) {
        registerIncoming(pedestalCode);

        // Parameters of the message
        String cardRfid = decodeAuthTag(request.authTag());
        Integer socketNo = request.socketNo();
        Integer meterStartWh = request.meterStartWh();

        // The socket must exist
        Socket socket = pedestalService.findSocket(pedestalCode, socketNo);
        if (socket == null) {
            throw new IllegalArgumentException(String.format("Socket %s of pedestal %s not found", socketNo,
                    pedestalCode));
        }

        // Process by the pedestal backend
        SupplyRequest supplyRequest = SupplyRequest.start(toKwh(meterStartWh), request.timestamp());
        SupplyResponse supplyResponse = supplyControlUnit.startSupply(SocketLinkId.of(pedestalCode, socketNo),
                cardRfid, supplyRequest);

        return new SupplyStartedResponse(supplyResponse.getTransactionId(), translateFrom(supplyResponse.getStatus()),
                supplyResponse.getExpiryDate(), supplyResponse.getMaxCurrentA(),
                supplyResponse.getPriceInformation(), supplyResponse.getGreeting());
    }

    @Transactional
    public SupplyStoppedResponse supplyStopped(String pedestalCode, SupplyStoppedRequest request) {
        registerIncoming(pedestalCode);

        String cardRfid = decodeAuthTag(request.authTag());
        Integer transactionId = request.transactionId();
        BigDecimal meterStopKwh = toKwh(request.meterStopWh());
        LocalDateTime stopTimestamp = request.timestamp();

        // Socket of the session, for monitoring (the session is closed by the stop)
        SupplySession session = transactionId != null
                ? supplySessionManager.findSessionFor(pedestalCode, transactionId) : null;

        // The pedestal backend books the stop; the pedestal always gets an answer it accepts
        SupplyResponse supplyResponse = supplyControlUnit.stopSupply(pedestalCode, cardRfid,
                SupplyRequest.stop(transactionId, meterStopKwh, stopTimestamp,
                        StopReason.fromPedestal(request.reason())));

        // monitoring
        if (session != null) {
            monitor(session.getSocket(), stopTimestamp);
        }

        return new SupplyStoppedResponse(translateFrom(supplyResponse.getStatus()), supplyResponse.getExpiryDate());
    }

    /**
     * After the stop the socket is free again.
     */
    private void monitor(Socket socket, LocalDateTime stopTimestamp) {
        try {
            socketStateTaskWorker.monitorSocket(socket, SocketStateParams.status(
                    stopTimestamp != null ? stopTimestamp : dateService.getNow(), SocketStatus.AVAILABLE, null));
        } catch (Exception e) {
            LOGGER.warn(e.getMessage(), e);
            LOGGER.error(String.format("Error during monitoring for socket (%s) and stop timestamp (%s)", socket,
                    stopTimestamp));
        }
    }

    @Transactional
    public AcknowledgeResponse socketStatus(String pedestalCode, SocketStatusRequest request) {

        Integer socketNo = request.socketNo();

        // the pedestal code is the identity on the link
        if (pedestalCode == null) {
            throw new IllegalArgumentException(String.format("Link v2 socket status without pedestal code (socket %s)",
                    socketNo));
        }

        // load the pedestal to find the manufacturer
        Pedestal pedestal = getPedestal(pedestalCode);
        Socket socket = pedestal.findSocketBySocketNo(socketNo);
        if (socket == null) {
            throw new IllegalArgumentException(String.format("Unknown socket %s of pedestal %s", socketNo,
                    pedestalCode));
        }
        LocalDateTime now = dateService.getNow();
        pedestalService.registerIncoming(pedestal, now);

        // the vendor error code first, then the error code of the link
        Long errorFlags = translateErrorCode(pedestal, request);

        SocketStatus status = translateStatus(request.status(), request.errorCode());

        socketStateTaskWorker.monitorSocket(socket, SocketStateParams.status(now, status, errorFlags));

        // Socket state event, kept for a retry when it can not be sent
        try {
            final Event statusEvent = new Event(EventType.SOCKET_STATE_NOTIFICATION);
            statusEvent.setAttribute(EventAttribute.PEDESTAL_ID, pedestal.getId());
            statusEvent.setAttribute(EventAttribute.SOCKET_ID, socket.getId());
            statusEvent.setAttribute(EventAttribute.SOCKET_NO, socketNo);
            statusEvent.setAttribute(EventAttribute.SOCKET_STATUS, status);
            statusEvent.setAttribute(EventAttribute.PEDESTAL_VENDOR, request.vendorId());
            statusEvent.setAttribute(EventAttribute.PEDESTAL_VENDOR_ERROR_CODE, request.vendorErrorCode() != null
                    ? request.vendorErrorCode() : NOT_SUPPORTED);
            statusEvent.setAttribute(EventAttribute.PEDESTAL_ERROR_CODE, request.errorCode());
            statusEvent.setAttribute(EventAttribute.PEDESTAL_INFO, request.info() != null ? request.info()
                    : NOT_SUPPORTED);
            statusEvent.setAttribute(EventAttribute.STATUS, request.status());
            statusEvent.setAttribute(EventAttribute.TIME, request.timestamp());
            statusEvent.setAttribute(EventAttribute.PEDESTAL_CODE, pedestal.getCode());
            statusEvent.setAttribute(EventAttribute.GROUP_ID, pedestal.getGroup() != null
                    ? pedestal.getGroup().getId() : null);
            LOGGER.debug(String.format("Socket state event %s handed to the event retry", statusEvent));
            eventRetryService.sendOrRetryLater(statusEvent);
        } catch (Exception e) {
            // the status is monitored already: the pedestal still gets its acknowledge
            LOGGER.error(String.format("Socket state event of socket %s of pedestal %s not sent", socketNo,
                    pedestalCode), e);
        }

        return new AcknowledgeResponse(now);
    }

    /**
     * Error flags of the status: the mask of the vendor error code for the manufacturer when it is an error, otherwise
     * the mask of the link error code (manufacturer row, then the link row, unknown codes as unknown error).
     */
    Long translateErrorCode(Pedestal pedestal, SocketStatusRequest request) {
        if (request.vendorErrorCode() != null && !request.vendorErrorCode().isBlank()) {
            String manufacturer = manufacturerOf(pedestal);
            Long vendorMask = socketStatusLogDao.findErrorMask(manufacturer, request.vendorErrorCode());
            if (isErrorCode(vendorMask)) {
                return vendorMask;
            }
            LOGGER.debug(String.format("Vendor error code %s of pedestal %s (%s) is no error, link error code %s used",
                    request.vendorErrorCode(), pedestal.getCode(), manufacturer, request.errorCode()));
        }
        return socketStateTaskWorker.translateErrorCode(pedestal, request.errorCode());
    }

    private static boolean isErrorCode(Long mask) {
        return mask != null && SocketStatusLog.NO_ERROR != mask;
    }

    /**
     * A pedestal not yet typed has no manufacturer: no vendor mask and no vendor data for it.
     */
    private static String manufacturerOf(Pedestal pedestal) {
        return pedestal.getPedestalType() != null ? pedestal.getPedestalType().getManufacturer() : null;
    }

    @Transactional
    public AcknowledgeResponse meterReadings(String pedestalCode, MeterReadingsRequest request) {
        registerIncoming(pedestalCode);

        Integer socketNo = request.socketNo();

        Socket socket = pedestalService.findSocket(pedestalCode, socketNo);
        if (socket == null) {
            throw new IllegalArgumentException(String.format("Socket %s of pedestal %s not found", socketNo,
                    pedestalCode));
        }

        processMeterReadings(socket, request);

        return new AcknowledgeResponse(dateService.getNow());
    }

    void processMeterReadings(Socket socket, MeterReadingsRequest request) {
        if (request.readings() == null) {
            return;
        }
        for (MeterReading meterReading : request.readings()) {
            BigDecimal meterKwh = toKwh(meterReading.valueWh());
            if (meterKwh == null) {
                LOGGER.warn(String.format("Meter reading without value of socket %s skipped", socket));
                continue;
            }
            LocalDateTime at = meterReading.timestamp() != null ? meterReading.timestamp() : dateService.getNow();
            LOGGER.debug(String.format("Meter reading %s kWh of socket %s at %s (transaction %s)", meterKwh,
                    socket, at, request.transactionId()));
            socketStateTaskWorker.monitorSocket(socket, SocketStateParams.meterReading(at, meterKwh));
        }
    }

    /**
     * Progress of a firmware update: only mailed to the service recipient of the pedestal.
     */
    @Transactional
    public AcknowledgeResponse firmwareStatus(String pedestalCode, FirmwareStatusRequest request) {
        registerIncoming(pedestalCode);
        LocalDateTime now = dateService.getNow();
        try {
            Pedestal pedestal = getPedestal(pedestalCode);
            pedestalMailService.sendStatusMail(pedestal, NotificationType.FIRMWARE_STATUS, request.status(), now);
        } catch (Exception e) {
            LOGGER.error(String.format("Firmware status %s of pedestal %s not mailed", request.status(),
                    pedestalCode), e);
        }
        return new AcknowledgeResponse(now);
    }

    /**
     * Result of a diagnostics upload: only mailed to the service recipient of the pedestal.
     */
    @Transactional
    public AcknowledgeResponse diagnosticsStatus(String pedestalCode, DiagnosticsStatusRequest request) {
        registerIncoming(pedestalCode);
        LocalDateTime now = dateService.getNow();
        try {
            Pedestal pedestal = getPedestal(pedestalCode);
            pedestalMailService.sendStatusMail(pedestal, NotificationType.DIAGNOSTICS_STATUS, request.status(), now);
        } catch (Exception e) {
            LOGGER.error(String.format("Diagnostics status %s of pedestal %s not mailed", request.status(),
                    pedestalCode), e);
        }
        return new AcknowledgeResponse(now);
    }

    /**
     * Data of the controller's vendor: accepted from the manufacturer of the pedestal type only and passed on as
     * event (kept for a retry), nothing is answered back.
     */
    @Transactional
    public VendorDataResponse vendorData(String pedestalCode, VendorDataRequest request) {
        Pedestal pedestal = getPedestal(pedestalCode);
        pedestalService.registerIncoming(pedestal, dateService.getNow());

        String manufacturer = manufacturerOf(pedestal);
        if (request.vendorId() == null || !request.vendorId().equalsIgnoreCase(manufacturer)) {
            LOGGER.warn(String.format("Vendor data of pedestal %s from vendor %s, but the manufacturer is %s",
                    pedestalCode, request.vendorId(), manufacturer));
            return new VendorDataResponse(VendorDataResponse.UNKNOWN_VENDOR, null);
        }
        if (request.messageId() == null || request.messageId().isBlank()) {
            return new VendorDataResponse(VendorDataResponse.UNKNOWN_MESSAGE_ID, null);
        }

        Event event = new Event(EventType.VENDOR_DATA);
        event.setAttribute(EventAttribute.PEDESTAL_ID, pedestal.getId());
        event.setAttribute(EventAttribute.PEDESTAL_CODE, pedestal.getCode());
        event.setAttribute(EventAttribute.PEDESTAL_VENDOR, request.vendorId());
        event.setAttribute(EventAttribute.VENDOR_MESSAGE_ID, request.messageId());
        event.setAttribute(EventAttribute.VENDOR_DATA, request.data());
        eventRetryService.sendOrRetryLater(event);

        return new VendorDataResponse(VendorDataResponse.ACCEPTED, null);
    }

    private Pedestal getPedestal(String pedestalCode) {
        Pedestal pedestal = pedestalService.findByCode(pedestalCode);
        if (pedestal == null) {
            throw new IllegalArgumentException("Unknown pedestal " + pedestalCode);
        }
        return pedestal;
    }

    private void registerIncoming(String pedestalCode) {
        Pedestal pedestal = pedestalService.findByCode(pedestalCode);
        if (pedestal != null) {
            pedestalService.registerIncoming(pedestal, dateService.getNow());
        }
    }

    private void sendQueuedLocalList(Pedestal pedestal) {
        if (pedestal != null && pedestalLinkClient.hasQueuedLocalList(pedestal)) {
            try {
                pedestalLinkClient.sendQueuedLocalList(pedestal);
            } catch (Exception e) {
                LOGGER.error(String.format("Sending the queued local card list to %s failed", pedestal), e);
            }
        }
    }

    /**
     * Link v2 sends meter values as whole Wh.
     */
    static BigDecimal toKwh(Integer valueWh) {
        return valueWh != null ? BigDecimal.valueOf(valueWh).movePointLeft(3) : null;
    }

    static String decodeAuthTag(String authTag) {
        return authTag != null ? authTag.trim() : null;
    }

    static String translateFrom(ValidationStatus status) {
        if (status == null) {
            return INVALID;
        }
        switch (status) {
            case ACCEPTED:
                return ACCEPTED;
            case CARD_INVALID:
            case CARD_NOT_ACCEPTED_AT_SITE:
            case SOCKET_INVALID:
                return INVALID;
            case SOCKET_OCCUPIED:
                return OCCUPIED;
            case EXPIRED:
                return EXPIRED;
            case BLOCKED:
                return BLOCKED;
            case UNAVAILABLE:
                return UNAVAILABLE;
            default:
                return INVALID;
        }
    }

    /**
     * Link v2 reports a tripped breaker itself; the v1 way (faulted with the breaker error code) is still understood.
     */
    static SocketStatus translateStatus(String status, String errorCode) {
        SocketStatus socketStatus = translateStatus(status);
        if (socketStatus == SocketStatus.FAULTED && BREAKER_TRIPPED.equals(errorCode)) {
            return SocketStatus.BREAKER_TRIPPED;
        }
        return socketStatus;
    }

    static SocketStatus translateStatus(String status) {
        if (status == null) {
            throw new IllegalArgumentException("Socket status is missing");
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
                return SocketStatus.FAULTED;
            case "UNAVAILABLE":
                return SocketStatus.UNAVAILABLE;
            case "FIRMWARE_UPDATE":
                return SocketStatus.FIRMWARE_UPDATE;
            default:
                throw new IllegalArgumentException("Unknown socket status " + status);
        }
    }
}
