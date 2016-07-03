package io.pillopl.supply;

import io.pillopl.card.CampingCard;
import io.pillopl.card.HouseCard;
import io.pillopl.card.ValidationService.ValidationResult;
import io.pillopl.guest.Stay;
import io.pillopl.infrastructure.PedestalService;
import io.pillopl.infrastructure.Socket;
import io.pillopl.persistence.dao.StayDao;
import io.pillopl.persistence.dao.SupplySessionDao;
import io.pillopl.power.PowerSection;
import io.pillopl.util.DateService;
import io.pillopl.value.QrPayAuthTag;
import io.pillopl.value.SocketLinkId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Supply sessions of the sockets: finds the session a pedestal message belongs to (a repeated message finds its
 * session again), creates and closes sessions, and answers the card authorization whether a socket is occupied.
 */
@Primary
@Service
public class SupplySessionManager implements SupplySessionRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(SupplySessionManager.class);

    private final SupplySessionDao supplySessionDao;

    private final PedestalService pedestalService;

    private final DateService dateService;

    private final StayDao stayDao;

    public SupplySessionManager(SupplySessionDao supplySessionDao, PedestalService pedestalService,
                                DateService dateService, StayDao stayDao) {
        this.supplySessionDao = supplySessionDao;
        this.pedestalService = pedestalService;
        this.dateService = dateService;
        this.stayDao = stayDao;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasConcurrentSession(final SocketLinkId socketLinkId, final String cardRfid) {
        Socket socket = pedestalService.findSocket(socketLinkId.getPedestalCode(), socketLinkId.socketNo());
        if (socket == null) {
            return false;
        }
        for (SupplySession session : supplySessionDao.findActiveBySocket(socket)) {
            if (!session.isStartedWith(cardRfid)) {
                LOG.info(String.format("Socket %s is occupied by session %s, card %s refused", socketLinkId, session,
                        cardRfid));
                return true;
            }
        }
        return false;
    }

    /**
     * Nothing is stored at authorization: the pedestal reports the start with its meter value and time, and only
     * then the session exists (an authorized guest may never plug in).
     */
    @Override
    public void createSession(final SocketLinkId socketLinkId, final String cardRfid) {
        LOG.debug(String.format("Card %s authorized at socket %s, session starts with the pedestal's supplyStarted",
                cardRfid, socketLinkId));
    }

    @Transactional(readOnly = true)
    public SupplySession getSession(Long sessionId) {
        return sessionId != null ? supplySessionDao.findById(sessionId).orElse(null) : null;
    }

    @Transactional(readOnly = true)
    public SupplySession getActiveSession(final Socket socket) {
        List<SupplySession> sessions = supplySessionDao.findActiveBySocket(socket);
        if (sessions.isEmpty()) {
            LOG.debug(String.format("No active session found for socket %s", socket));
            return null;
        }
        if (sessions.size() > 1) {
            LOG.warn(String.format("Socket %s has %d active sessions, using the newest %s", socket, sessions.size(),
                    sessions.get(0)));
        }
        return sessions.get(0);
    }

    /**
     * Active sessions of all sockets hanging on the section switchboard.
     */
    @Transactional(readOnly = true)
    public List<SupplySession> getActiveSessions(final PowerSection section) {
        return supplySessionDao.findActiveByPowerSection(section);
    }

    @Transactional(readOnly = true)
    public SupplySession findSessionFor(SocketLinkId socketLinkId, String cardRfid, Integer transactionId,
                                        boolean forHouseCard) {
        final Socket socket = getSocket(socketLinkId);
        final Long qrPayProcessId = QrPayAuthTag.parse(cardRfid);
        final SupplySession session = forHouseCard
                ? supplySessionDao.findHouseCardSessionBy(socket, cardRfid, transactionId)
                : qrPayProcessId != null
                ? supplySessionDao.findQrPaySessionBy(socket, qrPayProcessId, transactionId)
                : supplySessionDao.findBy(socket, cardRfid, transactionId);
        LOG.debug(String.format("For socket (%s), card (%s) and transaction id (%s) found session (%s)",
                socketLinkId, cardRfid, transactionId, session));
        return session;
    }

    /**
     * The pedestal repeats a supplyStarted it got no answer for: the same socket, card, start meter and remote date
     * mean the same session.
     */
    @Transactional(readOnly = true)
    public SupplySession findSessionFor(SocketLinkId socketLinkId, String cardRfid, BigDecimal startMeterKwh,
                                        LocalDateTime remoteDate, boolean forHouseCard) {
        final Socket socket = getSocket(socketLinkId);
        final Long qrPayProcessId = QrPayAuthTag.parse(cardRfid);
        final List<SupplySession> sessions = forHouseCard
                ? supplySessionDao.findHouseCardSessionsBy(socket, cardRfid, startMeterKwh, remoteDate)
                : qrPayProcessId != null
                ? supplySessionDao.findQrPaySessionsBy(socket, qrPayProcessId, startMeterKwh, remoteDate)
                : supplySessionDao.findBy(socket, cardRfid, startMeterKwh, remoteDate);
        final SupplySession session = sessions.isEmpty() ? null : sessions.get(0);
        LOG.debug(String.format("Found session (%s) for socket (%s), card (%s), start meter (%s) and remote date (%s).",
                session, socketLinkId, cardRfid, startMeterKwh, remoteDate));
        return session;
    }

    @Transactional(readOnly = true)
    public SupplySession findSessionFor(String pedestalCode, Integer transactionId) {
        final SupplySession session = supplySessionDao.findByPedestalCodeAndTransactionId(pedestalCode,
                transactionId);
        LOG.debug(String.format("Found session (%s) for pedestal (%s) and transaction id (%s).", session,
                pedestalCode, transactionId));
        return session;
    }

    /**
     * @param transactionId the pedestal's own transaction number, or null to draw one from the sequence
     */
    @Transactional
    public SupplySession createNewSessionFor(ValidationResult validationResult, Integer transactionId,
                                             BigDecimal startMeterKwh, LocalDateTime remoteDate) {
        CampingCard card = validationResult.getCard();
        HouseCard houseCard = validationResult.getHouseCard();
        Socket socket = validationResult.getSocket();

        LOG.info(String.format(
                "Creating new session for socket (%s), card (%s), house card (%s), transaction id (%s), start meter (%s), remote date (%s)",
                socket, card, houseCard, transactionId, startMeterKwh, remoteDate));
        final SupplySession session = new SupplySession();
        // the backend clock, not the pedestal one
        session.setCreateDate(dateService.getNow());
        // set the pedestal's values
        session.setRemoteDate(remoteDate);
        session.setSocket(socket);
        session.setStartMeterKwh(startMeterKwh);
        // set the right card
        if (validationResult.isHouseCardAccepted()) {
            session.setHouseCard(houseCard);
        } else if (validationResult.isQrPayAccepted()) {
            // remote supply start of a QR payment: no card
            session.setQrPayProcessId(validationResult.getQrPayProcessId());
        } else {
            session.setCard(card);
            // the stay of the guest: its check-out closes the session, its unit is the one supplied
            final Stay stay = findStayFor(card, remoteDate != null ? remoteDate : session.getCreateDate());
            if (stay != null) {
                session.setStayId(stay.getId());
                session.setUnitPlate(stay.getUnitPlate());
            }
        }
        // session number for the pedestal
        if (transactionId == null) {
            session.setTransactionId(supplySessionDao.nextTransactionId().intValue());
        } else {
            session.setTransactionId(transactionId);
        }
        // the breaker of the socket limits the current
        session.setMaxCurrentA(socket.getMaxCurrentInAmperes());
        session.setActive(true);

        SupplySession savedSession = supplySessionDao.save(session);
        LOG.info(String.format("Created new session (%s)", savedSession));
        return savedSession;
    }

    @Transactional
    public boolean deactivate(final SupplySession session, final StopReason stopReason, final LocalDateTime at) {
        if (session == null) {
            LOG.warn(String.format("No session to deactivate with %s", stopReason));
            return false;
        }
        LOG.debug(String.format("Deactivate session: %s (%s)", session, stopReason));
        session.close(stopReason, at);
        supplySessionDao.save(session);
        return true;
    }

    /**
     * Active sessions whose socket and pedestal have been silent for the timeout: no meter reading, no status and
     * no message from the pedestal. A supply over several nights keeps reporting, so it is never stale.
     */
    @Transactional(readOnly = true)
    public List<SupplySession> findStaleSessions(LocalDateTime now, int timeoutInHours) {
        return supplySessionDao.findActiveSilentSince(now.minusHours(timeoutInHours));
    }

    @Transactional(readOnly = true)
    public List<SupplySession> findActiveSessionsWithStay() {
        return supplySessionDao.findActiveWithStay();
    }

    /**
     * The open stay the card was handed out for on the day of the supply; null for cards without a stay (seasonal
     * residents, issuer cards).
     */
    private Stay findStayFor(final CampingCard card, final LocalDateTime at) {
        if (card == null || card.getId() == null) {
            return null;
        }
        final List<Stay> stays = stayDao.findOpenStaysOfCardOn(card.getId(), at.toLocalDate());
        if (stays.isEmpty()) {
            LOG.debug(String.format("No open stay of card %s on %s", card, at));
            return null;
        }
        if (stays.size() > 1) {
            LOG.info(String.format("Card %s has %d open stays on %s, the session belongs to stay %s", card,
                    stays.size(), at.toLocalDate(), stays.get(0).getId()));
        }
        return stays.get(0);
    }

    private Socket getSocket(final SocketLinkId socketLinkId) {
        Socket socket = pedestalService.findSocket(socketLinkId.getPedestalCode(), socketLinkId.socketNo());
        if (socket == null) {
            throw new IllegalArgumentException(String.format("Either the pedestal or the socket of (%s) is unknown",
                    socketLinkId));
        }
        return socket;
    }
}
