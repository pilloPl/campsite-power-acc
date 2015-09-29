package io.pillopl.jobs;

import io.pillopl.config.SystemPropertyService;
import io.pillopl.event.EventRetryService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Sends the events kept in EVENT_RETRY again.
 */
@DisallowConcurrentExecution
public class EventRetryJob extends QuartzJobBean {

    private final EventRetryService eventRetryService;

    private final SystemPropertyService systemPropertyService;

    public EventRetryJob(EventRetryService eventRetryService, SystemPropertyService systemPropertyService) {
        this.eventRetryService = eventRetryService;
        this.systemPropertyService = systemPropertyService;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) {
        eventRetryService.retryOpenEvents(systemPropertyService.getEventRetryMaxAttempts());
    }
}
