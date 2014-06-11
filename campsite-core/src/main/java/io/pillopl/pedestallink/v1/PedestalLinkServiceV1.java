package io.pillopl.pedestallink.v1;

import io.pillopl.card.Authorization;
import io.pillopl.card.AuthorizationResult;
import io.pillopl.card.ValidationStatus;
import io.pillopl.config.SystemPropertyService;
import io.pillopl.dto.pedestallink.v1.AcknowledgeResponse;
import io.pillopl.dto.pedestallink.v1.AuthorizeRequest;
import io.pillopl.dto.pedestallink.v1.AuthorizeResponse;
import io.pillopl.dto.pedestallink.v1.BootRequest;
import io.pillopl.dto.pedestallink.v1.BootResponse;
import io.pillopl.dto.pedestallink.v1.MeterReading;
import io.pillopl.dto.pedestallink.v1.MeterReadingsRequest;
import io.pillopl.dto.pedestallink.v1.SocketStatusRequest;
import io.pillopl.dto.pedestallink.v1.SupplyStartedRequest;
import io.pillopl.dto.pedestallink.v1.SupplyStartedResponse;
import io.pillopl.dto.pedestallink.v1.SupplyStoppedRequest;
import io.pillopl.dto.pedestallink.v1.SupplyStoppedResponse;
import io.pillopl.event.Event;
import io.pillopl.event.EventAttribute;
import io.pillopl.event.EventService;
import io.pillopl.event.EventType;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.PedestalService;
import io.pillopl.infrastructure.Socket;
import io.pillopl.infrastructure.SocketStatus;
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
 * First generation of the pedestal link: one method per message of the pedestal controller. Every message moves the
 * link state of the pedestal; boot and heartbeat also deliver a local card list queued for the pedestal.
 */
@Service
public class PedestalLinkServiceV1 {

    private static final Logger LOGGER = LoggerFactory.getLogger(PedestalLinkServiceV1.class);

    static final String ACCEPTED = "ACCEPTED";

    static final String REJECTED = "REJECTED";

    static final String BLOCKED = "BLOCKED";

    static final String EXPIRED = "EXPIRED";

    static final String INVALID = "INVALID";

    static final String OCCUPIED = "OCCUPIED";

    private static final String NOT_SUPPORTED = "";

    private final Authorization authorization;

    private final DateService dateService;

    private final SystemPropertyService systemPropertyService;

    private final SupplyControlUnitImpl supplyControlUnit;

    private final SupplySessionManager supplySessionManager;

    private final PedestalService pedestalService;

    private final EventService eventService;

    private final PedestalLinkClientV1 pedestalLinkClient;

    public PedestalLinkServiceV1(Authorization authorization, DateService dateService,
                                 SystemPropertyService systemPropertyService, SupplyControlUnitImpl supplyControlUnit,
                                 SupplySessionManager supplySessionManager, PedestalService pedestalService,
                                 EventService eventService, PedestalLinkClientV1 pedestalLinkClient) {
        this.authorization = authorization;
        this.dateService = dateService;
        this.systemPropertyService = systemPropertyService;
        this.supplyControlUnit = supplyControlUnit;
        this.supplySessionManager = supplySessionManager;
        this.pedestalService = pedestalService;
        this.eventService = eventService;
        this.pedestalLinkClient = pedestalLinkClient;
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
            pedestalService.updateSocketStatus(socket, SocketStatus.AVAILABLE,
                    stopTimestamp != null ? stopTimestamp : dateService.getNow());
        } catch (Exception e) {
            LOGGER.warn(e.getMessage(), e);
            LOGGER.error(String.format("Error during monitoring for socket (%s) and stop timestamp (%s)", socket,
                    stopTimestamp));
        }
    }

    @Transactional
    public AcknowledgeResponse socketStatus(String pedestalCode, SocketStatusRequest request) {

        Integer socketNo = request.socketNo();

        // load the pedestal and its socket
        Pedestal pedestal = pedestalService.findByCode(pedestalCode);
        Socket socket = pedestal != null ? pedestal.findSocketBySocketNo(socketNo) : null;
        if (socket == null) {
            throw new IllegalArgumentException(String.format("Unknown socket %s of pedestal %s", socketNo,
                    pedestalCode));
        }
        LocalDateTime now = dateService.getNow();
        pedestalService.registerIncoming(pedestal, now);

        SocketStatus status = translateStatus(request.status());
        pedestalService.updateSocketStatus(socket, status, now);

        // Socket state event
        try {
            final Event statusEvent = new Event(EventType.SOCKET_STATE_NOTIFICATION);
            statusEvent.setAttribute(EventAttribute.PEDESTAL_ID, pedestal.getId());
            statusEvent.setAttribute(EventAttribute.SOCKET_ID, socket.getId());
            statusEvent.setAttribute(EventAttribute.SOCKET_NO, socketNo);
            statusEvent.setAttribute(EventAttribute.SOCKET_STATUS, status);
            statusEvent.setAttribute(EventAttribute.PEDESTAL_VENDOR_ERROR_CODE, NOT_SUPPORTED);
            statusEvent.setAttribute(EventAttribute.PEDESTAL_ERROR_CODE, request.errorCode());
            statusEvent.setAttribute(EventAttribute.PEDESTAL_INFO, request.info() != null ? request.info()
                    : NOT_SUPPORTED);
            statusEvent.setAttribute(EventAttribute.STATUS, request.status());
            statusEvent.setAttribute(EventAttribute.PEDESTAL_CODE, pedestal.getCode());
            statusEvent.setAttribute(EventAttribute.GROUP_ID, pedestal.getGroup() != null
                    ? pedestal.getGroup().getId() : null);
            eventService.send(statusEvent);
        } catch (Exception e) {
            // log error
            LOGGER.error(e.getMessage(), e);
        }

        return new AcknowledgeResponse(now);
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
            LocalDateTime at = meterReading.timestamp() != null ? meterReading.timestamp() : dateService.getNow();
            if (pedestalService.updateMeterReading(socket, meterKwh, at)) {
                LOGGER.debug(String.format("Meter reading %s kWh of socket %s at %s (transaction %s)", meterKwh,
                        socket, at, request.transactionId()));
            }
        }
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
     * Link v1 sends meter values as whole Wh.
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
                return INVALID;
            default:
                return INVALID;
        }
    }

    static SocketStatus translateStatus(String status) {
        if (status == null) {
            throw new IllegalArgumentException("Socket status is missing");
        }
        switch (status) {
            case "AVAILABLE":
                return SocketStatus.AVAILABLE;
            case "FAULTED":
                return SocketStatus.FAULTED;
            case "OCCUPIED":
                return SocketStatus.SUPPLYING;
            case "UNAVAILABLE":
                return SocketStatus.UNAVAILABLE;
            default:
                throw new IllegalArgumentException("Unknown socket status " + status);
        }
    }
}
