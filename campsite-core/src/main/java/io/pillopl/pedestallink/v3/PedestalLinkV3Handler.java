package io.pillopl.pedestallink.v3;

import io.pillopl.card.AuthorizationResult;
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
import io.pillopl.event.EventType;
import io.pillopl.group.attribute.NotificationType;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.Socket;
import io.pillopl.infrastructure.SocketStatus;
import io.pillopl.monitoring.SocketStateParams;
import io.pillopl.monitoring.SocketStatusLog;
import io.pillopl.pedestallink.v3.PedestalLinkServiceV3.TranslationHelper;
import io.pillopl.supply.StopReason;
import io.pillopl.supply.SupplyRequest;
import io.pillopl.supply.SupplyResponse;
import io.pillopl.supply.SupplySession;
import io.pillopl.value.SocketLinkId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Handler of one message of the pedestal link v3. The handlers get everything they need through the locator getters
 * of {@link PedestalLinkServiceV3}; they are registered there and called by message class. The handlers of the
 * link messages are the nested classes.
 *
 * @param <REQ>  body of the message
 * @param <RESP> answer to the controller
 */
public abstract class PedestalLinkV3Handler<REQ, RESP> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PedestalLinkV3Handler.class);

    private static final String NOT_SUPPORTED = "";

    protected final PedestalLinkServiceV3 service;

    protected PedestalLinkV3Handler(PedestalLinkServiceV3 service) {
        this.service = service;
    }

    public abstract RESP processRequest(REQ request, String pedestalCode);

    public static class Authorize extends PedestalLinkV3Handler<AuthorizeRequest, AuthorizeResponse> {

        public Authorize(PedestalLinkServiceV3 service) {
            super(service);
        }

        @Override
        public AuthorizeResponse processRequest(AuthorizeRequest request, String pedestalCode) {
            service.registerIncoming(pedestalCode);

            String cardRfid = TranslationHelper.decodeAuthTag(request.authTag());

            AuthorizationResult authorizationResult = request.socketNo() != null
                    ? service.getAuthorization().authenticate(cardRfid,
                    SocketLinkId.of(pedestalCode, request.socketNo()))
                    : service.getAuthorization().authenticate(cardRfid, pedestalCode);

            return new AuthorizeResponse(TranslationHelper.toLinkStatus(
                    authorizationResult.getStatus()), authorizationResult.getExpiryDate(),
                    authorizationResult.getGuestName());
        }
    }

    public static class Boot extends PedestalLinkV3Handler<BootRequest, BootResponse> {

        public Boot(PedestalLinkServiceV3 service) {
            super(service);
        }

        @Override
        public BootResponse processRequest(BootRequest request, String pedestalCode) {
            Pedestal pedestal = service.getPedestalService().updateBootData(pedestalCode, request.firmwareVersion(),
                    request.serialNumber(), request.modemIccid());
            LocalDateTime now = service.getDateService().getNow();
            if (pedestal != null) {
                service.getPedestalService().registerIncoming(pedestal, now);
            }

            BootResponse bootResponse = new BootResponse(pedestal != null ? TranslationHelper.ACCEPTED
                    : TranslationHelper.REJECTED, now,
                    service.getSystemPropertyService().getPedestalHeartbeatIntervalInSeconds());

            if (pedestal != null) {
                Event event = new Event(EventType.BOOT_NOTIFICATION);
                event.setAttribute(EventAttribute.PEDESTAL_ID, pedestal.getId());
                event.setAttribute(EventAttribute.PEDESTAL_CODE, pedestal.getCode());
                event.setAttribute(EventAttribute.FIRMWARE_VERSION, request.firmwareVersion());
                event.setAttribute(EventAttribute.MODEM_ICCID, request.modemIccid());
                event.setAttribute(EventAttribute.METER_SERIAL_NUMBER, request.meterSerialNumber());
                event.setAttribute(EventAttribute.PEDESTAL_SERIAL_NUMBER, request.serialNumber());
                event.setAttribute(EventAttribute.PEDESTAL_VENDOR, request.vendor());
                event.setAttribute(EventAttribute.PEDESTAL_MODEL, request.model());
                event.setAttribute(EventAttribute.TIME, bootResponse.currentTime());
                event.setAttribute(EventAttribute.STATUS, bootResponse.status());
                event.setAttribute(EventAttribute.HEARTBEAT_INTERVAL, bootResponse.heartbeatInterval());
                service.getEventRetryService().sendOrRetryLater(event);
            }
            service.sendQueuedLocalList(pedestal);
            return bootResponse;
        }
    }

    public static class Heartbeat extends PedestalLinkV3Handler<Void, AcknowledgeResponse> {

        public Heartbeat(PedestalLinkServiceV3 service) {
            super(service);
        }

        @Override
        public AcknowledgeResponse processRequest(Void request, String pedestalCode) {
            LocalDateTime modDate = service.getDateService().getNow();
            Pedestal pedestal = service.getPedestalService().findByCode(pedestalCode);
            if (pedestal != null) {
                service.getPedestalService().registerIncoming(pedestal, modDate);
                service.sendQueuedLocalList(pedestal);
            } else {
                LOGGER.warn(String.format("Link v3 heartbeat of unknown pedestal %s", pedestalCode));
            }
            return new AcknowledgeResponse(modDate);
        }
    }

    public static class SupplyStarted extends PedestalLinkV3Handler<SupplyStartedRequest, SupplyStartedResponse> {

        public SupplyStarted(PedestalLinkServiceV3 service) {
            super(service);
        }

        @Override
        public SupplyStartedResponse processRequest(SupplyStartedRequest request, String pedestalCode) {
            service.registerIncoming(pedestalCode);

            // the socket must exist
            service.getSocket(pedestalCode, request.socketNo());

            SupplyResponse supplyResponse = service.getSupplyControlUnit().startSupply(
                    SocketLinkId.of(pedestalCode, request.socketNo()),
                    TranslationHelper.decodeAuthTag(request.authTag()),
                    SupplyRequest.start(TranslationHelper.toKwh(request.meterStartWh()), request.timestamp()));

            return new SupplyStartedResponse(supplyResponse.getTransactionId(),
                    TranslationHelper.toLinkStatus(supplyResponse.getStatus()),
                    supplyResponse.getExpiryDate(), supplyResponse.getMaxCurrentA(),
                    supplyResponse.getPriceInformation(), supplyResponse.getGreeting());
        }
    }

    public static class SupplyStopped extends PedestalLinkV3Handler<SupplyStoppedRequest, SupplyStoppedResponse> {

        public SupplyStopped(PedestalLinkServiceV3 service) {
            super(service);
        }

        @Override
        public SupplyStoppedResponse processRequest(SupplyStoppedRequest request, String pedestalCode) {
            service.registerIncoming(pedestalCode);

            Integer transactionId = request.transactionId();
            SupplySession session = transactionId != null
                    ? service.getSupplySessionManager().findSessionFor(pedestalCode, transactionId) : null;

            SupplyResponse supplyResponse = service.getSupplyControlUnit().stopSupply(pedestalCode,
                    TranslationHelper.decodeAuthTag(request.authTag()),
                    SupplyRequest.stop(transactionId, TranslationHelper.toKwh(request.meterStopWh()),
                            request.timestamp(), StopReason.fromPedestal(request.reason())));

            if (session != null) {
                try {
                    service.getSocketStateTaskWorker().monitorSocket(session.getSocket(), SocketStateParams.status(
                            request.timestamp() != null ? request.timestamp() : service.getDateService().getNow(),
                            SocketStatus.AVAILABLE, null));
                } catch (Exception e) {
                    LOGGER.error(String.format("Link v3 monitoring after the stop of transaction %s failed",
                            transactionId), e);
                }
            }

            return new SupplyStoppedResponse(TranslationHelper.toLinkStatus(
                    supplyResponse.getStatus()), supplyResponse.getExpiryDate());
        }
    }

    /**
     * Socket status of link v3: socket 0 is the status of the pedestal itself, only passed on as event; a socket
     * status is checked against the former status (an unexpected change is only warned about), monitored, and its
     * event goes out through the event retry.
     */
    public static class SocketStatusHandler extends PedestalLinkV3Handler<SocketStatusRequest, AcknowledgeResponse> {

        public SocketStatusHandler(PedestalLinkServiceV3 service) {
            super(service);
        }

        @Override
        public AcknowledgeResponse processRequest(SocketStatusRequest request, String pedestalCode) {
            if (request.socketNo() == null) {
                throw new IllegalArgumentException(String.format(
                        "Link v3 socket status of pedestal %s without socket number", pedestalCode));
            }
            int socketNo = request.socketNo();

            SocketStatus status = TranslationHelper.translateSocketStatus(request.status(), request.errorCode());

            Pedestal pedestal = service.getPedestal(pedestalCode);
            LocalDateTime now = service.getDateService().getNow();
            service.getPedestalService().registerIncoming(pedestal, now);

            Socket socket = null;
            if (isSocket(socketNo)) {
                socket = service.getSocket(pedestalCode, socketNo);
                if (!TranslationHelper.isExpectedTransition(socket.getStatus(), status)) {
                    LOGGER.warn(String.format("Pedestal %s reports socket %d going from %s to %s", pedestalCode,
                            socketNo, socket.getStatus(), status));
                }
                service.getSocketStateTaskWorker().monitorSocket(socket, SocketStateParams.status(now, status,
                        translateErrorCode(pedestal, request)));
            }

            final Event statusEvent = new Event(EventType.SOCKET_STATE_NOTIFICATION);
            statusEvent.setAttribute(EventAttribute.PEDESTAL_ID, pedestal.getId());
            statusEvent.setAttribute(EventAttribute.SOCKET_ID, socket != null ? socket.getId() : null);
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
            service.getEventRetryService().sendOrRetryLater(statusEvent);

            return new AcknowledgeResponse(now);
        }

        private static boolean isSocket(int socketNo) {
            return 0 != socketNo;
        }

        private Long translateErrorCode(Pedestal pedestal, SocketStatusRequest request) {
            // a pedestal not yet typed has no manufacturer mask: only the link error code counts
            String manufacturer = pedestal.getPedestalType() != null
                    ? pedestal.getPedestalType().getManufacturer() : null;
            String vendorErrorCode = request.vendorErrorCode();
            if (manufacturer != null && vendorErrorCode != null && !vendorErrorCode.isBlank()) {
                Long vendorMask = service.getSocketStatusLogDao().findErrorMask(manufacturer, vendorErrorCode);
                if (vendorMask != null && SocketStatusLog.NO_ERROR != vendorMask) {
                    return vendorMask;
                }
            }
            return service.getSocketStateTaskWorker().translateErrorCode(pedestal, request.errorCode());
        }
    }

    public static class MeterReadings extends PedestalLinkV3Handler<MeterReadingsRequest, AcknowledgeResponse> {

        public MeterReadings(PedestalLinkServiceV3 service) {
            super(service);
        }

        @Override
        public AcknowledgeResponse processRequest(MeterReadingsRequest request, String pedestalCode) {
            service.registerIncoming(pedestalCode);

            Socket socket = service.getSocket(pedestalCode, request.socketNo());

            if (request.readings() != null) {
                for (MeterReading meterReading : request.readings()) {
                    BigDecimal meterKwh = TranslationHelper.toKwh(meterReading.valueWh());
                    if (meterKwh == null) {
                        LOGGER.warn(String.format("Link v3 meter reading without value of socket %s skipped",
                                socket));
                        continue;
                    }
                    LocalDateTime at = meterReading.timestamp() != null ? meterReading.timestamp()
                            : service.getDateService().getNow();
                    service.getSocketStateTaskWorker().monitorSocket(socket,
                            SocketStateParams.meterReading(at, meterKwh));
                }
            }
            return new AcknowledgeResponse(service.getDateService().getNow());
        }
    }

    public static class FirmwareStatus extends PedestalLinkV3Handler<FirmwareStatusRequest, AcknowledgeResponse> {

        public FirmwareStatus(PedestalLinkServiceV3 service) {
            super(service);
        }

        @Override
        public AcknowledgeResponse processRequest(FirmwareStatusRequest request, String pedestalCode) {
            return service.mailStatus(pedestalCode, NotificationType.FIRMWARE_STATUS, request.status());
        }
    }

    public static class DiagnosticsStatus
            extends PedestalLinkV3Handler<DiagnosticsStatusRequest, AcknowledgeResponse> {

        public DiagnosticsStatus(PedestalLinkServiceV3 service) {
            super(service);
        }

        @Override
        public AcknowledgeResponse processRequest(DiagnosticsStatusRequest request, String pedestalCode) {
            return service.mailStatus(pedestalCode, NotificationType.DIAGNOSTICS_STATUS, request.status());
        }
    }

    public static class VendorData extends PedestalLinkV3Handler<VendorDataRequest, VendorDataResponse> {

        public VendorData(PedestalLinkServiceV3 service) {
            super(service);
        }

        @Override
        public VendorDataResponse processRequest(VendorDataRequest request, String pedestalCode) {
            Pedestal pedestal = service.getPedestal(pedestalCode);
            service.getPedestalService().registerIncoming(pedestal, service.getDateService().getNow());

            String manufacturer = pedestal.getPedestalType() != null
                    ? pedestal.getPedestalType().getManufacturer() : null;
            if (request.vendorId() == null || !request.vendorId().equalsIgnoreCase(manufacturer)) {
                LOGGER.warn(String.format("Link v3 vendor data of pedestal %s from vendor %s, but the manufacturer"
                        + " is %s", pedestalCode, request.vendorId(), manufacturer));
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
            service.getEventRetryService().sendOrRetryLater(event);

            return new VendorDataResponse(VendorDataResponse.ACCEPTED, null);
        }
    }
}
