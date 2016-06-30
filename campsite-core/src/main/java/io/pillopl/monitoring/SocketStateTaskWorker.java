package io.pillopl.monitoring;

import io.pillopl.event.Event;
import io.pillopl.event.EventAttribute;
import io.pillopl.event.EventService;
import io.pillopl.event.EventType;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.Socket;
import io.pillopl.infrastructure.SocketStatus;
import io.pillopl.persistence.dao.SocketDao;
import io.pillopl.persistence.dao.SocketStatusLogDao;
import io.pillopl.util.DateService;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Monitoring of a socket: applies what a pedestal message tells about it, keeps the monitoring log and raises an
 * event and a service mail when the socket shows a new class of error.
 */
@Component
public class SocketStateTaskWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(SocketStateTaskWorker.class);

    static final String UNKNOWN_ERROR_CODE = "UNKNOWN_ERROR";

    private final SocketDao socketDao;

    private final SocketStatusLogDao socketStatusLogDao;

    private final EventService eventService;

    private final PedestalMailService pedestalMailService;

    private final DateService dateService;

    public SocketStateTaskWorker(SocketDao socketDao, SocketStatusLogDao socketStatusLogDao,
                                 EventService eventService, PedestalMailService pedestalMailService,
                                 DateService dateService) {
        this.socketDao = socketDao;
        this.socketStatusLogDao = socketStatusLogDao;
        this.eventService = eventService;
        this.pedestalMailService = pedestalMailService;
        this.dateService = dateService;
    }

    @Transactional
    public void monitorSocket(final Socket socket, final SocketStateParams params) {
        if (params == null) {
            throw new AssertionError("SocketStateParams == null");
        }
        params.validate();

        if (socket == null) {
            LOGGER.warn(String.format("Something happened to the socket of the message %s. It can not be loaded.",
                    params));
            return;
        }

        // remember the firmware the pedestal reports
        if (params.firmwareVersion() != null && !params.firmwareVersion().equals(socket.getFirmwareVersion())) {
            socket.setFirmwareVersion(params.firmwareVersion());
            LOGGER.debug(String.format("Set firmware version %s for socket %s", socket.getFirmwareVersion(),
                    socket.getName()));
        }

        // error gone: watch the pedestal again
        if (params.status() == SocketStatus.AVAILABLE && (socket.getStatus() == SocketStatus.FAULTED
                || socket.getStatus() == SocketStatus.BREAKER_TRIPPED || socket.getStatus() == SocketStatus.UNKNOWN)) {
            socket.setMonitoring(true);
            LOGGER.debug(String.format("Set monitoring %s for socket %s", socket.isMonitoring(), socket.getName()));
        }

        final LocalDateTime serverDate = dateService.getNow();

        // update attributes
        if (params.status() != null) {
            final SocketStatus formerStatus = socket.getStatus();
            if (params.status() != formerStatus) {
                socket.setLastStatusUpdate(serverDate);
            }
            socket.setStatus(params.status());
            LOGGER.debug(String.format("Set status %s for socket %s", socket.getStatus(), socket.getName()));
        }

        // the time of the message, not of the server: closing stale sessions takes it as the last sign of life
        socket.setModDate(params.timestamp());
        LOGGER.debug(String.format("Set modification date %s for socket %s", socket.getModDate(), socket.getName()));

        // update meter reading
        if (params.meterKwh() != null && params.meterKwh().signum() != -1) {
            socket.setMeterKwh(params.meterKwh());
            LOGGER.debug(String.format("Set meter reading %s for socket %s", socket.getMeterKwh(), socket.getName()));
        } else if (params.meterKwh() != null) {
            LOGGER.warn(String.format("Meter reading %s of socket %s skipped", params.meterKwh(), socket));
        }

        SocketStatusLog log = socketStatusLogDao.findLastBySocketId(socket.getId());

        if (isStatusUpdateNeeded(params, socket)) {
            final SocketStatus lastStatus = SocketStatusLog.lastStatusOrAvailable(log);
            socket.setStatus(lastStatus);
            LOGGER.debug(String.format("Set status %s for socket %s", socket.getStatus(), socket.getName()));
        }

        final long formerErrorFlags = log != null ? log.getErrorFlags() : SocketStatusLog.NO_ERROR;

        socketDao.save(socket);

        createAndSaveStatusLog(params, socket, log, formerErrorFlags, serverDate);

        // mail in error case: only when the socket was not in error before with the same reason
        if (errorChangedSinceLastReport(params, socket, formerErrorFlags)) {
            try {
                // send event
                sendEvent(socket);

                // send mail
                sendMail(params, socket);
            } catch (Exception e) {
                // log error
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Error flags of an error code sent by the pedestal: the mask of the manufacturer, otherwise the mask of the
     * link protocol; a code nobody knows counts as unknown error.
     *
     * @return the flags, or null when the message carried no error code
     */
    @Transactional(readOnly = true)
    public Long translateErrorCode(Pedestal pedestal, String errorCode) {
        if (errorCode == null || errorCode.isBlank()) {
            return null;
        }
        String manufacturer = pedestal.getPedestalType().getManufacturer();
        Long mask = socketStatusLogDao.findErrorMask(manufacturer, errorCode);
        if (mask == null) {
            LOGGER.warn(String.format("Unknown error code %s of pedestal %s (%s)", errorCode, pedestal.getCode(),
                    manufacturer));
            mask = socketStatusLogDao.findErrorMask(manufacturer, UNKNOWN_ERROR_CODE);
        }
        return mask;
    }

    private static boolean errorChangedSinceLastReport(SocketStateParams params, Socket socket,
                                                            long formerErrorFlags) {
        return (socket.getStatus() == SocketStatus.FAULTED || socket.getStatus() == SocketStatus.BREAKER_TRIPPED)
                && params.errorFlags() != null
                && params.errorFlags() != formerErrorFlags
                && SocketStatusLog.NO_ERROR != params.errorFlags();
    }

    private static boolean isStatusUpdateNeeded(SocketStateParams params, Socket socket) {
        return params.status() == null && socket.isMonitoring();
    }

    private void createAndSaveStatusLog(SocketStateParams params, Socket socket, SocketStatusLog lastLog,
                                        long formerErrorFlags, LocalDateTime serverDate) {
        // pedestals out of monitoring raise no alarm
        if (!socket.isMonitoring()) {
            return;
        }
        SocketStatusLog log = lastLog;
        if (isNewStatusLogNeeded(log, socket.getStatus(), params.errorFlags(), params.currentA())) {
            log = new SocketStatusLog(socket, serverDate, params.timestamp());
            log.setStatus(socket.getStatus());
            log.setMeterKwh(socket.getMeterKwh());
            // a message without error flags (meter reading, heartbeat) keeps the former flags
            log.setErrorFlags(params.errorFlags() != null ? params.errorFlags() : formerErrorFlags);
            log.setCurrentA(params.currentA());
            LOGGER.debug(String.format("Create status log %s for socket %s", log, socket.getName()));
        } else {
            log.setUpdateDate(serverDate);
            if (params.meterKwh() != null && params.meterKwh().signum() != -1
                    && log.getMeterKwh().compareTo(params.meterKwh()) != 0) {
                log.setMeterKwh(params.meterKwh());
                LOGGER.debug(String.format("Update meter reading %s for status log of socket %s", params.meterKwh(),
                        socket.getName()));
            }
        }
        socketStatusLogDao.save(log);
    }

    private static boolean isNewStatusLogNeeded(SocketStatusLog log, SocketStatus status, Long errorFlags,
                                                BigDecimal currentA) {
        if (log == null) {
            LOGGER.debug("New status log is needed because the last status log is null");
            return true;
        }
        if (log.getStatus() != status) {
            LOGGER.debug(String.format("New status log is needed because the last status is %s and the new one %s",
                    log.getStatus(), status));
            return true;
        }
        if (errorFlags != null && !Objects.equals(log.getErrorFlags(), errorFlags)) {
            LOGGER.debug(String.format("New status log is needed because the last error flags are %s and the new"
                    + " ones %s", log.getErrorFlags(), errorFlags));
            return true;
        }
        if (log.getCurrentA() != null && currentA != null && log.getCurrentA().compareTo(currentA) != 0) {
            LOGGER.debug(String.format("New status log is needed because the last current is %s and the new one %s",
                    log.getCurrentA(), currentA));
            return true;
        }
        return false;
    }

    private void sendEvent(final Socket socket) {
        final Event statusEvent = new Event(EventType.SOCKET_STATUS_CHANGED);
        statusEvent.setAttribute(EventAttribute.SOCKET_ID, socket.getId());
        statusEvent.setAttribute(EventAttribute.PEDESTAL_ID, socket.getPedestal().getId());
        statusEvent.setAttribute(EventAttribute.SOCKET_STATUS, socket.getStatus());
        eventService.send(statusEvent);
    }

    private void sendMail(final SocketStateParams params, final Socket socket) throws MessagingException {
        pedestalMailService.sendErrorMail(socket, params.errorFlags());
    }
}
