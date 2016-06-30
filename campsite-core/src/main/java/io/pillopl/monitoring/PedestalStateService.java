package io.pillopl.monitoring;

import io.pillopl.event.Event;
import io.pillopl.event.EventAttribute;
import io.pillopl.event.EventService;
import io.pillopl.event.EventType;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.PedestalState;
import io.pillopl.infrastructure.SocketStatus;
import io.pillopl.persistence.dao.PedestalStateDao;
import io.pillopl.persistence.dao.SocketStatusLogDao;
import io.pillopl.util.DateService;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Link state of the pedestals over time: pedestals without a message for too long are timed out (mail and UNKNOWN
 * event) and keep getting UNKNOWN events while they stay silent. Called by the heartbeat timeout job and the unknown
 * state event trigger job; a run handles at most {@link #MAX_PEDESTALS_TO_TIMEOUT} pedestals. Also refreshes the
 * utilization view built from the monitoring log.
 */
@Service
public class PedestalStateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PedestalStateService.class);

    static final int MAX_PEDESTALS_TO_TIMEOUT = 25;

    private final PedestalStateDao pedestalStateDao;

    private final PedestalMailService pedestalMailService;

    private final EventService eventService;

    private final DateService dateService;

    private final SocketStatusLogDao socketStatusLogDao;

    public PedestalStateService(PedestalStateDao pedestalStateDao, PedestalMailService pedestalMailService,
                                EventService eventService, DateService dateService,
                                SocketStatusLogDao socketStatusLogDao) {
        this.pedestalStateDao = pedestalStateDao;
        this.pedestalMailService = pedestalMailService;
        this.eventService = eventService;
        this.dateService = dateService;
        this.socketStatusLogDao = socketStatusLogDao;
    }

    /**
     * Times out the pedestals alive until now whose last message is older than the timeout.
     */
    @Transactional
    public Report timeoutPedestals(final int silenceLimitMinutes) {
        LocalDateTime now = dateService.getNow();
        LOGGER.info(String.format("Looking for timed out pedestals since %s minute(s)", silenceLimitMinutes));
        List<PedestalState> states = pedestalStateDao.findAliveWithLastIncomingBefore(
                now.minusMinutes(silenceLimitMinutes));
        logIfTooManyPedestals(states);

        Report report = new Report(Report.TIMEOUT_PEDESTALS);
        for (int i = 0; i < Math.min(states.size(), MAX_PEDESTALS_TO_TIMEOUT); i++) {
            PedestalState state = states.get(i);
            if (state.checkTimeout(now, silenceLimitMinutes)) {
                pedestalStateDao.save(state);
                report.timedOutPedestals++;
                sendEmail(state, report);
                sendUnknownEvent(state.getPedestal(), report);
            }
        }

        report.log();
        return report;
    }

    /**
     * Sends UNKNOWN events for the pedestals silent for longer than the unknown state timeout.
     */
    @Transactional(readOnly = true)
    public Report sendUnknownEvents(final int silenceLimitMinutes) {
        List<PedestalState> states = pedestalStateDao.findWithLastIncomingBefore(
                dateService.getNow().minusMinutes(silenceLimitMinutes));
        logIfTooManyPedestals(states);

        Report report = new Report(Report.UNKNOWN_EVENTS);
        for (int i = 0; i < Math.min(states.size(), MAX_PEDESTALS_TO_TIMEOUT); i++) {
            sendUnknownEvent(states.get(i).getPedestal(), report);
        }

        report.log();
        return report;
    }

    /**
     * Refreshes the utilization view of the sockets (MV SOCKET_UTILIZATION) from the monitoring log.
     */
    @Transactional
    public void refreshSocketUtilization() {
        LOGGER.info("Refreshing SOCKET_UTILIZATION");
        socketStatusLogDao.refreshSocketUtilization();
    }

    void sendUnknownEvent(Pedestal pedestal, Report report) {
        try {
            final Event statusEvent = new Event(EventType.SOCKET_STATUS_CHANGED);
            statusEvent.setAttribute(EventAttribute.PEDESTAL_ID, pedestal.getId());
            statusEvent.setAttribute(EventAttribute.PEDESTAL_CODE, pedestal.getCode());
            statusEvent.setAttribute(EventAttribute.SOCKET_STATUS, SocketStatus.UNKNOWN);
            eventService.send(statusEvent);
            report.sentEvents++;
        } catch (RuntimeException e) {
            LOGGER.error("Pedestal state event lost: " + e.getMessage(), e);
        }
    }

    void sendEmail(PedestalState state, Report report) {
        try {
            pedestalMailService.sendTimeoutMail(state.getPedestal(), state.getApproxLastIncoming());
            report.sentMails++;
        } catch (MessagingException | RuntimeException e) {
            LOGGER.error("Pedestal state mail lost: " + e.getMessage(), e);
        }
    }

    private static void logIfTooManyPedestals(List<PedestalState> states) {
        if (states.size() > MAX_PEDESTALS_TO_TIMEOUT) {
            LOGGER.error(String.format("We have many pedestals (%s) in timeout! Only handling %s pedestals...",
                    states.size(), MAX_PEDESTALS_TO_TIMEOUT));
        }
    }

    /**
     * Statistics of one run, logged at its end.
     */
    public static final class Report {

        static final String TIMEOUT_PEDESTALS = "TimeoutPedestals";

        static final String UNKNOWN_EVENTS = "SendUnknownEvents";

        private final String name;

        private int timedOutPedestals;

        private int sentMails;

        private int sentEvents;

        private Report(String name) {
            this.name = name;
        }

        public int getTimedOutPedestals() {
            return timedOutPedestals;
        }

        public int getSentMails() {
            return sentMails;
        }

        public int getSentEvents() {
            return sentEvents;
        }

        private void log() {
            LOGGER.info(String.format("Report statistics for: '%s' || %s pedestals timed out | %s mails sent"
                    + " | %s events sent (one per pedestal) ||", name, timedOutPedestals, sentMails, sentEvents));
        }
    }
}
