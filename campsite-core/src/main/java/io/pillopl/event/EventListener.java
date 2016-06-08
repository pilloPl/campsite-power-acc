package io.pillopl.event;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base of the event listeners: registers its own queue for one event type at start-up and takes the events from
 * it on a single thread; the work itself usually goes as an {@link EventTask} to the listener's executor.
 */
public abstract class EventListener {

    private static final Logger LOG = LoggerFactory.getLogger(EventListener.class);

    private static final long POLL_TIMEOUT_IN_MILLIS = 500;

    private final EventService eventService;

    private final EventTaskExecutorFactory executorFactory;

    private final AtomicBoolean alive = new AtomicBoolean(true);

    private final ExecutorService takeFromQueueExecutor = Executors.newSingleThreadExecutor();

    private final AtomicInteger loggingCounter = new AtomicInteger(0);

    private AsyncTaskExecutor taskExecutor;

    private BlockingQueue<Event> queue;

    protected EventListener(EventService eventService, EventTaskExecutorFactory executorFactory) {
        this.eventService = eventService;
        this.executorFactory = executorFactory;
    }

    public abstract String getName();

    public abstract EventType getType();

    public abstract void handleEvent(final Event event);

    private class TakeFromQueueTask implements Runnable {
        @Override
        public final void run() {
            LOG.info("TakeFromQueueTask of " + getName() + " running");

            while (alive.get()) {
                Event event = null;
                try {
                    event = queue.poll(POLL_TIMEOUT_IN_MILLIS, TimeUnit.MILLISECONDS);
                    if (event != null) {
                        handleEvent(event);
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception ex) {
                    LOG.error("Event handling failed with an exception for " + createLogStringFrom(event), ex);
                }
            }
            LOG.info("TakeFromQueueTask of " + getName() + " exit");
        }
    }

    @PostConstruct
    public final void postcreate() {
        final String eventSystemName = EventService.eventSourceName(getName());

        eventService.registerQueue(eventSystemName, getType());
        queue = eventService.subscribeToQueue(eventSystemName);

        LOG.info("Registered " + getName());

        this.taskExecutor = newListenerExecutor(executorFactory);

        // fire and forget: derived classes just handle the events
        takeFromQueueExecutor.execute(new TakeFromQueueTask());
    }

    protected AsyncTaskExecutor newListenerExecutor(EventTaskExecutorFactory factory) {
        return factory.singleWorkerExecutor(getName());
    }

    public Future<?> submitTask(final EventTask eventTask) {
        try {
            return taskExecutor.submit(eventTask);
        } finally {
            logExecutorState();
        }
    }

    protected void execute(final EventTask task) {
        try {
            taskExecutor.execute(task);
        } finally {
            logExecutorState();
        }
    }

    private void logExecutorState() {
        if (loggingCounter.incrementAndGet() % 5 == 0 && LOG.isInfoEnabled()) {
            LOG.info(getTaskExecutorState(taskExecutor));
        }
    }

    protected String createLogStringFrom(final Event event) {
        StringBuilder builder = new StringBuilder();
        builder.append("Queue: ").append(EventService.eventSourceName(getName()));
        if (null != event) {
            builder.append(event);
        } else {
            builder.append(", Event: [null]");
        }
        return builder.toString();
    }

    protected String getTaskExecutorState(AsyncTaskExecutor e) {
        if (e instanceof ThreadPoolTaskExecutor tpte) {
            return String.format("%s pool: size %d, busy %d, waiting %d", getClass().getName(),
                    tpte.getPoolSize(), tpte.getActiveCount(), tpte.getThreadPoolExecutor().getQueue().size());
        }
        return "No pool statistics for executor " + e;
    }

    @PreDestroy
    public void tryShutdown() {
        LOG.info(String.format("Stopping event listener %s for %s (%s)", getName(), getType(), getClass().getName()));
        alive.set(false);
        eventService.unregisterQueue(EventService.eventSourceName(getName()), getType());
        takeFromQueueExecutor.shutdown();
        if (taskExecutor instanceof ThreadPoolTaskExecutor tpte) {
            tpte.shutdown();
        }
    }
}
