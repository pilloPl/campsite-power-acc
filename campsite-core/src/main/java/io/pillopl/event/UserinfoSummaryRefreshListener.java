package io.pillopl.event;

import io.pillopl.user.UserinfoSummaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Refreshes the materialized view USERINFO_SUMMARY when users change, so that the self-service list does not wait
 * for the next run of the refresh job. Changes arriving while a refresh is pending share that refresh.
 */
@Component
public class UserinfoSummaryRefreshListener extends EventListener {

    private static final Logger LOG = LoggerFactory.getLogger(UserinfoSummaryRefreshListener.class);

    private final UserinfoSummaryService userinfoSummaryService;

    private final AtomicBoolean refreshPending = new AtomicBoolean(false);

    public UserinfoSummaryRefreshListener(EventService eventService, EventTaskExecutorFactory executorFactory,
                                          UserinfoSummaryService userinfoSummaryService) {
        super(eventService, executorFactory);
        this.userinfoSummaryService = userinfoSummaryService;
    }

    @Override
    public String getName() {
        return "userinfoSummaryRefreshListener";
    }

    @Override
    public EventType getType() {
        return EventType.USERINFO_CHANGED;
    }

    @Override
    public void handleEvent(Event event) {
        if (!refreshPending.compareAndSet(false, true)) {
            LOG.debug("USERINFO_SUMMARY refresh already pending, " + event.getId() + " shares it");
            return;
        }
        execute(new EventTask(event) {
            @Override
            public void handleEvent() {
                refreshPending.set(false);
                userinfoSummaryService.refresh();
            }
        });
    }
}
