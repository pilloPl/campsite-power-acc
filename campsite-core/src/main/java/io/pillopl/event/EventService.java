package io.pillopl.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * In-process event queues. Every listener owns a named queue registered for one event type; {@link #send(Event)}
 * offers the event to every queue registered for its type (fire and forget, a full queue only logs).
 */
@Service
public class EventService {

    private static final Logger LOG = LoggerFactory.getLogger(EventService.class);

    static final int QUEUE_MAX_SIZE = 100000;

    private static final String PREFIX = "eventsysname_";

    private final Map<EventType, Set<String>> queueNamesByType = new ConcurrentHashMap<>();

    private final Map<String, BlockingQueue<Event>> queues = new ConcurrentHashMap<>();

    public static String eventSourceName(final String postfix) {
        return PREFIX + postfix;
    }

    public void send(final Event event) {
        offer(event);
    }

    /**
     * Like {@link #send(Event)}, but tells whether every queue registered for the type took the event. No registered
     * queue is no failure: nobody listens to the type.
     */
    public boolean offer(final Event event) {
        Set<String> queueNames = queueNamesByType.get(event.getType());
        if (queueNames == null || queueNames.isEmpty()) {
            LOG.debug(String.format("No queue registered for %s, event %s dropped", event.getType(), event.getId()));
            return true;
        }
        boolean allTaken = true;
        for (String queueName : queueNames) {
            allTaken &= putEventToQueue(event, queueName);
        }
        return allTaken;
    }

    public BlockingQueue<Event> subscribeToQueue(final String name) {
        return queues.computeIfAbsent(name, n -> new LinkedBlockingQueue<>(QUEUE_MAX_SIZE));
    }

    public void registerQueue(final String name, final EventType eventType) {
        queueNamesByType.computeIfAbsent(eventType, t -> new CopyOnWriteArraySet<>()).add(name);
    }

    public void unregisterQueue(final String name, final EventType eventType) {
        Set<String> names = queueNamesByType.get(eventType);
        if (names != null) {
            names.remove(name);
        }
    }

    public int getQueueSize(final String name) {
        BlockingQueue<Event> queue = queues.get(name);
        return queue != null ? queue.size() : 0;
    }

    private boolean putEventToQueue(Event event, String queueName) {
        BlockingQueue<Event> queue = subscribeToQueue(queueName);
        boolean successful = queue.offer(event);
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("queued=%s event %s on %s", successful, event, queueName));
        }
        if (!successful) {
            LOG.warn(String.format("Event %s rejected by queue %s, it is likely full", event, queueName));
        }
        return successful;
    }
}
