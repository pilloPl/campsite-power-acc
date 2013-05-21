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

    public static String createEventSystemName(final String postfix) {
        return PREFIX + postfix;
    }

    public void send(final Event event) {
        Set<String> queueNames = queueNamesByType.get(event.getType());
        if (queueNames == null || queueNames.isEmpty()) {
            LOG.debug(String.format("No queue registered for %s, event %s dropped", event.getType(), event.getId()));
            return;
        }
        for (String queueName : queueNames) {
            putEventToQueue(event, queueName);
        }
    }

    public BlockingQueue<Event> getQueueAndRegisterListener(final String name) {
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

    private void putEventToQueue(Event event, String queueName) {
        BlockingQueue<Event> queue = getQueueAndRegisterListener(queueName);
        boolean successful = queue.offer(event);
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("%s Offered %s into the queue %s", successful, event, queueName));
        }
        if (!successful) {
            LOG.warn(String.format("Can not offer event %s to queue %s. Probably the queue is full?", event, queueName));
        }
    }
}
