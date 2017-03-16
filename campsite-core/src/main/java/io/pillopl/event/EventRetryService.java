package io.pillopl.event;

import io.pillopl.persistence.dao.EventRetryDao;
import io.pillopl.util.DateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Sends events that must not get lost: a send that fails is written to EVENT_RETRY (in its own transaction, so the
 * row survives a rollback of the caller) and sent again by the retry job.
 */
@Service
public class EventRetryService {

    private static final Logger LOG = LoggerFactory.getLogger(EventRetryService.class);

    static final String QUEUE_REFUSED = "Event queue refused the event";

    private final EventService eventService;

    private final EventRetryDao eventRetryDao;

    private final DateService dateService;

    public EventRetryService(EventService eventService, EventRetryDao eventRetryDao, DateService dateService) {
        this.eventService = eventService;
        this.eventRetryDao = eventRetryDao;
        this.dateService = dateService;
    }

    /**
     * @return true when the event was sent now, false when it was kept for a retry (or could not even be kept)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean sendOrRetryLater(Event event) {
        String failure = trySend(event);
        if (failure == null) {
            return true;
        }
        try {
            EventRetry retry = eventRetryDao.save(EventRetry.of(event, failure, dateService.getNow()));
            LOG.error(String.format("Sending %s failed (%s), kept for a retry as %s", event, failure, retry));
        } catch (RuntimeException e) {
            // the message of the pedestal must still be answered: only log
            LOG.error(String.format("Sending %s failed (%s) and it could not be kept for a retry", event, failure), e);
        }
        return false;
    }

    /**
     * Sends the open events again, oldest first. An event failing for the max attempts time is given up (FAILED).
     *
     * @return number of events sent
     */
    @Transactional
    public int retryOpenEvents(int maxAttempts) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("Max attempts of the event retry must be at least 1, was "
                    + maxAttempts);
        }
        List<EventRetry> open = eventRetryDao.findByStatusOrderByIdAsc(EventRetry.Status.OPEN);
        int sent = 0;
        for (EventRetry retry : open) {
            String failure;
            try {
                failure = trySend(retry.toEvent());
            } catch (RuntimeException e) {
                // attributes that can not be read back fail every time; a failure without message is no success
                failure = describe(e);
            }
            if (failure == null) {
                retry.sent(dateService.getNow());
                sent++;
                LOG.info(String.format("%s sent on retry", retry));
            } else {
                retry.failedAgain(failure, dateService.getNow(), maxAttempts);
                if (retry.getStatus() == EventRetry.Status.FAILED) {
                    LOG.error(String.format("%s given up after %d attempts: %s", retry, retry.getRetryCount(),
                            failure));
                } else {
                    LOG.warn(String.format("%s failed again: %s", retry, failure));
                }
            }
            eventRetryDao.save(retry);
        }
        return sent;
    }

    private String trySend(Event event) {
        try {
            return eventService.offer(event) ? null : QUEUE_REFUSED;
        } catch (RuntimeException e) {
            return describe(e);
        }
    }

    private static String describe(RuntimeException e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getName();
    }
}
