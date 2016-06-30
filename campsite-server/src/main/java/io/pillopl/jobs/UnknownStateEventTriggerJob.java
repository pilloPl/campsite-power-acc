package io.pillopl.jobs;

import io.pillopl.config.SystemPropertyService;
import io.pillopl.monitoring.PedestalStateService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Keeps sending UNKNOWN events for the pedestals silent for longer than the unknown state timeout.
 */
@DisallowConcurrentExecution
public class UnknownStateEventTriggerJob extends QuartzJobBean {

    private final PedestalStateService pedestalStateService;

    private final SystemPropertyService systemPropertyService;

    public UnknownStateEventTriggerJob(PedestalStateService pedestalStateService,
                                       SystemPropertyService systemPropertyService) {
        this.pedestalStateService = pedestalStateService;
        this.systemPropertyService = systemPropertyService;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) {
        pedestalStateService.sendUnknownEvents(systemPropertyService.getMinutesUntilUnknownState());
    }
}
