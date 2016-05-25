package io.pillopl.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Work done for one event on the executor of a listener. Failures are logged, slow handling is warned about.
 */
public abstract class EventTask implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(EventTask.class);

    // 30 seconds
    public static final long DURATION_THRESHOLD_IN_MILLIS = 30L * 1000L;

    protected final Event event;

    private long startTimeInMillis;

    protected EventTask(Event event) {
        this.event = event;
    }

    public abstract void handleEvent();

    @Override
    public void run() {
        startTimeInMillis = System.currentTimeMillis();
        try {
            handleEvent();
        } catch (Exception e) {
            LOG.warn(String.format("Event %s not handled: %s", event, e.getMessage()));
        }
        long duration = System.currentTimeMillis() - startTimeInMillis;
        if (duration > DURATION_THRESHOLD_IN_MILLIS) {
            LOG.warn(String.format("Slow event handling of %s: %s ms", event, duration));
        }
        LOG.debug(String.format("Handled %s in %s ms", event, duration));
    }

    public Event getEvent() {
        return event;
    }
}
