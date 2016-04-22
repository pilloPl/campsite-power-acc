package io.pillopl.supply;

import io.pillopl.card.CampingCard;
import io.pillopl.card.ValidationService;
import io.pillopl.card.ValidationService.ValidationResult;
import io.pillopl.card.ValidationStatus;
import io.pillopl.config.SystemPropertyService;
import io.pillopl.consumption.ConsumptionRecord;
import io.pillopl.consumption.ConsumptionRecordWriteService;
import io.pillopl.event.Event;
import io.pillopl.event.EventAttribute;
import io.pillopl.event.EventService;
import io.pillopl.event.EventType;
import io.pillopl.infrastructure.PedestalService;
import io.pillopl.infrastructure.Socket;
import io.pillopl.persistence.dao.SupplyIncidentDao;
import io.pillopl.pricing.PricingService;
import io.pillopl.supply.SupplyIncident.IncidentType;
import io.pillopl.util.DateService;
import io.pillopl.value.SocketLinkId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Start and stop of the supply at a socket, as reported by the pedestal link: validates the card, finds or creates
 * the session, answers the pedestal, and on the stop determines consumption and duration, decides whether the
 * consumption is billed and closes the session. Sessions the pedestal never stopped are closed by a job.
 */
@Service
public class SupplyControlUnitImpl {

    private static final Logger LOG = LoggerFactory.getLogger(SupplyControlUnitImpl.class);

    /**
     * Year of the date the controllers send after a power loss (their clock restarts at the epoch).
     */
    static final int EPOCH_ZERO_YEAR = 1970;

    private final DateService dateService;

    private final SupplySessionManager sessionManager;

    private final ValidationService validationService;

    private final PedestalService pedestalService;

    private final PricingService pricingService;

    private final EventService eventService;

    private final SupplyIncidentDao supplyIncidentDao;

    private final SystemPropertyService systemPropertyService;

    private final ObjectProvider<CheckedOutStayFinder> checkedOutStayFinder;

    private final ConsumptionRecordWriteService consumptionRecordWriteService;

    private final TransactionTemplate transactionTemplate;

    public SupplyControlUnitImpl(DateService dateService, SupplySessionManager sessionManager,
                                 ValidationService validationService, PedestalService pedestalService,
                                 PricingService pricingService, EventService eventService,
                                 SupplyIncidentDao supplyIncidentDao, SystemPropertyService systemPropertyService,
                                 ObjectProvider<CheckedOutStayFinder> checkedOutStayFinder,
                                 ConsumptionRecordWriteService consumptionRecordWriteService,
                                 PlatformTransactionManager transactionManager) {
        this.dateService = dateService;
        this.sessionManager = sessionManager;
        this.validationService = validationService;
        this.pedestalService = pedestalService;
        this.pricingService = pricingService;
        this.eventService = eventService;
        this.supplyIncidentDao = supplyIncidentDao;
        this.systemPropertyService = systemPropertyService;
        this.checkedOutStayFinder = checkedOutStayFinder;
        this.consumptionRecordWriteService = consumptionRecordWriteService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Transactional
    public SupplyResponse startSupply(final SocketLinkId socketLinkId, final String cardRfid,
                                      final SupplyRequest supplyRequest) {

        LOG.debug(String.format("Starting supply for socket %s and card %s with %s", socketLinkId, cardRfid,
                supplyRequest));

        final LocalDateTime systemDate = dateService.getNow();
        final SupplyResponse startSupplyResponse = new SupplyResponse();
        final BigDecimal startMeterKwh = supplyRequest.getStartMeterKwh();
        final LocalDateTime remoteDate = adjustEpochZeroAfterPowerLoss(supplyRequest.getStartTimestamp(), systemDate);

        // May the card supply at this socket?
        final ValidationResult validationResult = validationService.validateFor(socketLinkId, cardRfid, remoteDate,
                false);
        final ValidationStatus status = validationResult.getStatus();

        startSupplyResponse.setStatus(status);
        if (cardIsNotNullAndHasValidTo(validationResult)) {
            startSupplyResponse.setExpiryDate(validationResult.getCard().getValidTo());
        }

        if (status != ValidationStatus.ACCEPTED) {
            // refused: the pedestal gets transaction id zero and must not supply
            startSupplyResponse.setTransactionId(0);
            LOG.info(String.format("Start of supply not accepted. Result (%s). Returning (%s).", validationResult,
                    startSupplyResponse));
            return startSupplyResponse;
        }

        // Did the pedestal backend process this start already (the pedestal repeats unanswered messages)?
        boolean alreadyProcessed = true;
        SupplySession session = sessionManager.findSessionFor(socketLinkId, cardRfid, startMeterKwh, remoteDate,
                validationResult.isHouseCardAccepted());
        if (session == null) {
            // not processed yet: new session (the session manager logs it)
            session = sessionManager.createNewSessionFor(validationResult, null, startMeterKwh, remoteDate);
            alreadyProcessed = false;
        }
        // Enrich the answer
        startSupplyResponse.setMaxCurrentA(session.getMaxCurrentA());
        startSupplyResponse.setTransactionId(session.getTransactionId());
        // price text of the tariff of the socket for camping cards, none for house cards
        startSupplyResponse.setPriceInformation(validationResult.isHouseCardAccepted() ? null
                : determinePriceInformation(session));
        startSupplyResponse.setGreeting(validationResult.isHouseCardAccepted() ? null
                : (session.getCard() != null && session.getCard().getGuest() != null)
                ? session.getCard().getGuest().getFullName() : null);

        if (alreadyProcessed) {
            // someone should look why the pedestal sends a processed start again
            LOG.error(String.format(
                    "Supply start for socket (%s), card (%s), start meter (%s), remote date (%s) was already processed for session (%s)! Ignoring it. Returning (%s)",
                    socketLinkId, cardRfid, startMeterKwh, remoteDate, session, startSupplyResponse));
            return startSupplyResponse;
        }

        LOG.debug(String.format("Start of supply accepted: %s", startSupplyResponse));
        createSupplyStartedEvent(socketLinkId, session, cardRfid);
        return startSupplyResponse;
    }

    /**
     * The pedestal stopped a supply. The pedestal always gets an answer it accepts: a stop the pedestal backend cannot
     * book (no transaction id, no session, session already closed) is ACCEPTED and logged as an incident.
     */
    @Transactional
    public SupplyResponse stopSupply(final String pedestalCode, final String cardRfid,
                                     final SupplyRequest supplyRequest) {

        final SupplyResponse stopSupplyResponse = new SupplyResponse();
        final LocalDateTime systemDate = dateService.getNow();
        final Integer transactionId = supplyRequest.getTransactionId();
        final ValidationResult validationResultOfSecondCard;

        if (transactionId == null) {
            return acceptWithIncident(IncidentType.NO_TRANSACTION_ID, pedestalCode, cardRfid, supplyRequest, null,
                    String.format("No transaction id in the supply stop of pedestal %s", pedestalCode),
                    stopSupplyResponse);
        }
        final SupplySession session = sessionManager.findSessionFor(pedestalCode, transactionId);

        if (session == null || !session.isActive()) {
            return acceptWithIncident(session == null ? IncidentType.NO_SESSION : IncidentType.SESSION_ALREADY_CLOSED,
                    pedestalCode, cardRfid, supplyRequest, session, getErrorMessageForSession(pedestalCode,
                            transactionId, session), stopSupplyResponse);
        }
        final String sessionCardRfid = session.getCardRfid();
        final SocketLinkId socketLinkId = socketLinkIdOf(session);

        // the supply ran already: the card of the session is only checked for a guest to bill, not validated
        final ValidationResult validationResult = validationService.validateFor(socketLinkId, sessionCardRfid, null,
                true);
        final ValidationStatus status;

        // another card stopped the supply: its answer is what the pedestal caches for that card
        if (wasDifferentCardUsage(cardRfid, sessionCardRfid, transactionId)) {
            validationResultOfSecondCard = validationService.validateFor(socketLinkId, cardRfid, null, false);
            status = validationResultOfSecondCard.getStatus();
        } else {
            status = validationResult.getStatus();
        }
        stopSupplyResponse.setStatus(status);

        if (cardIsNotNullAndHasValidTo(validationResult)) {
            stopSupplyResponse.setExpiryDate(validationResult.getCard().getValidTo());
        }
        // the pedestal backend started the session, so it should know the socket and the card
        if (status != ValidationStatus.ACCEPTED) {
            LOG.error(String.format(
                    "Supply stop validation for socket (%s), card (%s), request (%s) failed with result (%s)! Returning (%s)",
                    socketLinkId, sessionCardRfid, supplyRequest, validationResult, stopSupplyResponse));
        }

        finishSession(new StopData(supplyRequest.withStartOf(session), validationResult, session, systemDate,
                sessionCardRfid, socketLinkId));

        return stopSupplyResponse;
    }

    /**
     * Closes the sessions the pedestals never stopped: sessions of stays checked out (after the configured delay)
     * with CHECKOUT, then sessions whose socket and pedestal were silent for longer than the configured timeout
     * with TIMEOUT. The stop reading is the socket's last meter reading. Every session is closed in its own
     * transaction.
     *
     * @return number of closed sessions
     */
    public int closeStaleSessions() {
        final LocalDateTime now = dateService.getNow();
        int closed = closeCheckedOutSessions(now);
        closed += closeTimedOutSessions(now);
        if (closed > 0) {
            LOG.info(String.format("Closed %d stale supply sessions", closed));
        }
        return closed;
    }

    int closeCheckedOutSessions(final LocalDateTime now) {
        final CheckedOutStayFinder finder = checkedOutStayFinder.getIfAvailable();
        if (finder == null) {
            LOG.debug("No stays known, no session is closed on check-out");
            return 0;
        }
        final List<SupplySession> sessions = sessionManager.findActiveSessionsWithStay();
        if (sessions.isEmpty()) {
            return 0;
        }
        final Set<Long> stayIds = sessions.stream().map(SupplySession::getStayId).collect(Collectors.toSet());
        final Set<Long> checkedOut = finder.findCheckedOutStayIds(stayIds,
                now.minusMinutes(systemPropertyService.getCheckoutSessionCloseDelayInMinutes()));
        LOG.debug(String.format("%d of %d stays with an active supply session are checked out", checkedOut.size(),
                stayIds.size()));
        int closed = 0;
        for (SupplySession session : sessions) {
            if (checkedOut.contains(session.getStayId()) && closeInOwnTransaction(session.getId(),
                    StopReason.CHECKOUT, now)) {
                closed++;
            }
        }
        return closed;
    }

    int closeTimedOutSessions(final LocalDateTime now) {
        final int timeoutInHours = systemPropertyService.getStaleSessionTimeoutInHours();
        int closed = 0;
        for (SupplySession session : sessionManager.findStaleSessions(now, timeoutInHours)) {
            if (closeInOwnTransaction(session.getId(), StopReason.TIMEOUT, now)) {
                closed++;
            }
        }
        return closed;
    }

    private boolean closeInOwnTransaction(final Long sessionId, final StopReason stopReason,
                                          final LocalDateTime now) {
        try {
            return Boolean.TRUE.equals(transactionTemplate.execute(status -> closeSession(sessionId, stopReason,
                    now)));
        } catch (RuntimeException e) {
            LOG.error(String.format("Closing supply session %s with %s failed", sessionId, stopReason), e);
            return false;
        }
    }

    private boolean closeSession(final Long sessionId, final StopReason stopReason, final LocalDateTime now) {
        final SupplySession session = sessionManager.getSession(sessionId);
        if (session == null || !session.isActive()) {
            return false;
        }
        final Socket socket = session.getSocket();
        final SocketLinkId socketLinkId = socketLinkIdOf(session);
        final String sessionCardRfid = session.getCardRfid();
        final SupplyRequest closeRequest = SupplyRequest.close(session, socket.getMeterKwh(),
                lastReadingDate(session, socket, now), stopReason);

        final ValidationResult validationResult = validationService.validateFor(socketLinkId, sessionCardRfid, null,
                true);
        LOG.info(String.format("Closing session %s with %s: %s", session, stopReason, closeRequest));
        finishSession(new StopData(closeRequest, validationResult, session, now, sessionCardRfid, socketLinkId));
        return true;
    }

    /**
     * The time of the socket's last reading when it lies within the session, else the closing time.
     */
    static LocalDateTime lastReadingDate(SupplySession session, Socket socket, LocalDateTime now) {
        LocalDateTime start = session.getRemoteDate() != null ? session.getRemoteDate() : session.getCreateDate();
        LocalDateTime reading = socket.getModDate();
        if (reading != null && start != null && !reading.isBefore(start) && !reading.isAfter(now)) {
            return reading;
        }
        return now;
    }

    private void finishSession(final StopData stopData) {
        final Socket socket = stopData.session.getSocket();
        if (stopData.validationResult.getSocket() == null) {
            // the socket was removed while supplying: the session still knows it
            stopData.validationResult.setSocket(socket);
        }
        final Consumption consumption = determineConsumption(socket, stopData.supplyRequest);
        final SupplyDuration duration = determineDuration(stopData.supplyRequest, stopData.session,
                stopData.systemDate);
        final boolean billed = shouldConsumptionBeBilled(stopData.validationResult);
        final StopReason stopReason = stopData.supplyRequest.getStopReason() != null
                ? stopData.supplyRequest.getStopReason() : StopReason.LOCAL;

        saveConsumption(stopData, consumption, duration, billed, stopReason);
        updatePedestal(socket, stopData.supplyRequest, duration);
        sessionManager.deactivate(stopData.session, stopReason, duration.plugOut());
        createSupplyStoppedEvent(stopData, consumption, duration, billed, stopReason);
    }

    /**
     * The one consumption record of the session: the single place for stops of the pedestal and sessions closed by
     * the job.
     */
    private void saveConsumption(final StopData stopData, final Consumption consumption,
                                 final SupplyDuration duration, final boolean billed, final StopReason stopReason) {
        LOG.info(String.format("Consumption of session %s: %s, %s, billed: %s", stopData.session, consumption,
                duration, billed));
        ConsumptionRecord record = consumptionRecordWriteService.createConsumptionRecord(stopData.session,
                stopData.validationResult, consumption, duration, billed, stopReason, stopData.systemDate);
        LOG.debug(String.format("Saved consumption record %s", record));
    }

    private void updatePedestal(final Socket socket, final SupplyRequest supplyRequest,
                                final SupplyDuration duration) {
        BigDecimal stopMeterKwh = supplyRequest.getStopMeterKwh();
        if (stopMeterKwh != null) {
            pedestalService.updateMeterReading(socket, stopMeterKwh, duration.plugOut());
        }
    }

    /**
     * The consumption is billed unless a house card supplied (the campsite's own use), the pedestal is not billable
     * or the socket is a free socket.
     */
    boolean shouldConsumptionBeBilled(ValidationResult validationResult) {

        boolean shouldConsumptionBeBilled = true;

        if (validationResult.isHouseCardAccepted()) {
            shouldConsumptionBeBilled = false;
        } else if (!validationResult.getSocket().getPedestal().isBillable() || validationResult.isFreeSocket()) {
            shouldConsumptionBeBilled = false;
        }

        return shouldConsumptionBeBilled;
    }

    public Consumption determineConsumption(Socket socket, SupplyRequest supplyRequest) {
        // on problems a meter value is the last known reading of the socket
        final boolean startMeterOk = isValidMeterValue(supplyRequest.getStartMeterKwh());
        final BigDecimal startMeterKwh = meterOrLastReading(supplyRequest.getStartMeterKwh(), socket, "start");
        final boolean stopMeterOk = isValidMeterValue(supplyRequest.getStopMeterKwh());
        final BigDecimal stopMeterKwh = meterOrLastReading(supplyRequest.getStopMeterKwh(), socket, "stop");

        BigDecimal consumptionKwh = null;
        if (startMeterOk && stopMeterOk) {
            consumptionKwh = stopMeterKwh.subtract(startMeterKwh, MathContext.DECIMAL64);
            if (consumptionKwh.signum() == -1) {
                LOG.error("Consumption must not be negative, but was: " + consumptionKwh);
            } else {
                LOG.info(String.format("Consumption: %s kWh", consumptionKwh));
            }
        } else {
            LOG.warn("Consumption not set because of problems with the start or stop meter values!");
        }

        return new Consumption(startMeterKwh, stopMeterKwh, consumptionKwh);
    }

    private static boolean isValidMeterValue(BigDecimal meterKwh) {
        return meterKwh != null && meterKwh.signum() != -1;
    }

    private static BigDecimal meterOrLastReading(BigDecimal meterKwh, Socket socket, String which) {
        if (isValidMeterValue(meterKwh)) {
            LOG.debug(String.format("Consumption: %s meter %s", which, meterKwh));
            return meterKwh;
        }
        LOG.info(String.format("Consumption: invalid %s meter, set to the last known reading %s of %s!", which,
                socket.getMeterKwh(), socket));
        return socket.getMeterKwh();
    }

    SupplyDuration determineDuration(SupplyRequest supplyRequest, SupplySession session, LocalDateTime systemDate) {
        LocalDateTime plugIn = null;
        if (session != null) {
            plugIn = session.getRemoteDate();
            LOG.debug(String.format("Duration: plug-in %s", plugIn));
        } else {
            LOG.info("Pedestal was not connected to the pedestal backend. Stop of supply without session!");
        }

        if (supplyRequest.getStartTimestamp() != null) {
            plugIn = supplyRequest.getStartTimestamp();
            LOG.debug(String.format("Duration: plug-in %s", plugIn));
        }

        LocalDateTime plugOut = adjustEpochZeroAfterPowerLoss(supplyRequest.getStopTimestamp(), systemDate);
        LOG.debug(String.format("Duration: plug-out %s", plugOut));
        if (plugIn != null && plugOut.isBefore(plugIn)) {
            LOG.error(String.format("Duration: plug-out %s is before plug-in %s", plugOut, plugIn));
        }

        Long durationInSeconds = null;
        if (plugIn != null) {
            durationInSeconds = java.time.Duration.between(plugIn, plugOut).getSeconds();
        }

        return new SupplyDuration(plugIn, plugOut, durationInSeconds);
    }

    /**
     * Controllers lose their clock with the power and then send dates of 1970 until they synchronise again. This is
     * a firmware issue; to keep the supply the date is replaced by the system date. A missing date is replaced too.
     */
    static LocalDateTime adjustEpochZeroAfterPowerLoss(LocalDateTime date, LocalDateTime replaceDate) {
        if (date == null || date.getYear() == EPOCH_ZERO_YEAR) {
            return replaceDate;
        }
        return date;
    }

    private String determinePriceInformation(SupplySession session) {
        CampingCard card = session.getCard();
        return pricingService.getPriceInformation(session.getSocket().getId(), card != null ? card.getId() : null,
                session.getRemoteDate());
    }

    private SupplyResponse acceptWithIncident(IncidentType incidentType, String pedestalCode, String cardRfid,
                                              SupplyRequest supplyRequest, SupplySession session, String message,
                                              SupplyResponse supplyResponse) {
        supplyResponse.setStatus(ValidationStatus.ACCEPTED);
        LOG.error(message);
        try {
            SupplyIncident incident = new SupplyIncident(incidentType, pedestalCode, message, dateService.getNow());
            incident.setTransactionId(supplyRequest.getTransactionId());
            incident.setCardRfid(cardRfid);
            incident.setSupplySessionId(session != null ? session.getId() : null);
            incident.setMeterKwh(supplyRequest.getStopMeterKwh());
            incident.setRemoteDate(supplyRequest.getStopTimestamp());
            supplyIncidentDao.save(incident);
        } catch (RuntimeException e) {
            LOG.error("Could not log the supply incident: " + message, e);
        }
        return supplyResponse;
    }

    private static String getErrorMessageForSession(String pedestalCode, Integer transactionId,
                                                    SupplySession session) {
        if (session == null) {
            return String.format("Can't find session of transaction %s on pedestal %s!", transactionId,
                    pedestalCode);
        }
        final StopReason stopReason = session.getStopReason();
        return String.format("Session %s of transaction %s already closed by the %s (%s)!", session.getId(),
                transactionId, stopReason != null && stopReason.isClosedBySystem() ? "system" : "pedestal", stopReason);
    }

    private static boolean wasDifferentCardUsage(String cardRfid, String sessionCardRfid, Integer transactionId) {
        boolean different = cardRfid != null && !cardRfid.isBlank() && !cardRfid.equals(sessionCardRfid);
        if (different) {
            LOG.info(String.format("Transaction %s started with card %s was stopped with card %s", transactionId,
                    sessionCardRfid, cardRfid));
        }
        return different;
    }

    private static boolean cardIsNotNullAndHasValidTo(ValidationResult validationResult) {
        return validationResult.getCard() != null && validationResult.getCard().getValidTo() != null;
    }

    private static SocketLinkId socketLinkIdOf(SupplySession session) {
        Socket socket = session.getSocket();
        return SocketLinkId.of(socket.getPedestal().getCode(), socket.getSocketNo());
    }

    private void createSupplyStartedEvent(SocketLinkId socketLinkId, SupplySession session, String cardRfid) {
        try {
            Event event = new Event(EventType.SUPPLY_STARTED);
            event.setAttribute(EventAttribute.SUPPLY_SESSION_ID, session.getId());
            event.setAttribute(EventAttribute.SOCKET_ID, session.getSocket().getId());
            event.setAttribute(EventAttribute.PEDESTAL_CODE, socketLinkId.getPedestalCode());
            event.setAttribute(EventAttribute.SOCKET_NO, socketLinkId.socketNo());
            event.setAttribute(EventAttribute.RFID, cardRfid);
            event.setAttribute(EventAttribute.TRANSACTION_ID, session.getTransactionId());
            event.setAttribute(EventAttribute.START_TIME, session.getRemoteDate());
            event.setAttribute(EventAttribute.START_METER_KWH, session.getStartMeterKwh());
            LOG.debug(String.format("Created SupplyStarted event %s", event));
            eventService.send(event);
        } catch (Exception e) {
            LOG.error("Error sending SupplyStarted event.", e);
        }
    }

    private void createSupplyStoppedEvent(StopData stopData, Consumption consumption, SupplyDuration duration,
                                          boolean billed, StopReason stopReason) {
        try {
            Event event = new Event(EventType.SUPPLY_STOPPED);
            event.setAttribute(EventAttribute.SUPPLY_SESSION_ID, stopData.session.getId());
            event.setAttribute(EventAttribute.SOCKET_ID, stopData.session.getSocket().getId());
            event.setAttribute(EventAttribute.PEDESTAL_CODE, stopData.socketLinkId.getPedestalCode());
            event.setAttribute(EventAttribute.SOCKET_NO, stopData.socketLinkId.socketNo());
            event.setAttribute(EventAttribute.RFID, stopData.cardRfid);
            event.setAttribute(EventAttribute.TRANSACTION_ID, stopData.session.getTransactionId());
            event.setAttribute(EventAttribute.START_TIME, duration.plugIn());
            event.setAttribute(EventAttribute.STOP_TIME, duration.plugOut());
            event.setAttribute(EventAttribute.START_METER_KWH, consumption.startMeterKwh());
            event.setAttribute(EventAttribute.STOP_METER_KWH, consumption.stopMeterKwh());
            event.setAttribute(EventAttribute.STOP_REASON, stopReason.name());
            event.setAttribute(EventAttribute.BILLABLE, billed);
            LOG.debug(String.format("Created SupplyStopped event %s", event));
            eventService.send(event);
        } catch (Exception e) {
            LOG.error("Error sending SupplyStopped event.", e);
        }
    }

    private static final class StopData {
        final SupplyRequest supplyRequest;
        final ValidationResult validationResult;
        final SupplySession session;
        final LocalDateTime systemDate;
        final String cardRfid;
        final SocketLinkId socketLinkId;

        StopData(SupplyRequest supplyRequest, ValidationResult validationResult, SupplySession session,
                 LocalDateTime systemDate, String cardRfid, SocketLinkId socketLinkId) {
            this.supplyRequest = supplyRequest;
            this.validationResult = validationResult;
            this.session = session;
            this.systemDate = systemDate;
            this.cardRfid = cardRfid;
            this.socketLinkId = socketLinkId;
        }
    }
}
