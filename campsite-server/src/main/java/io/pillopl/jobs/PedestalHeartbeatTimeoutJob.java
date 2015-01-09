package io.pillopl.jobs;

import io.pillopl.config.SystemPropertyService;
import io.pillopl.monitoring.PedestalStateService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Times out the pedestals that sent no message within the heartbeat timeout (mail and UNKNOWN event).
 */
@DisallowConcurrentExecution
public class PedestalHeartbeatTimeoutJob extends QuartzJobBean {

    private final PedestalStateService pedestalStateService;

    private final SystemPropertyService systemPropertyService;

    public PedestalHeartbeatTimeoutJob(PedestalStateService pedestalStateService,
                                       SystemPropertyService systemPropertyService) {
        this.pedestalStateService = pedestalStateService;
        this.systemPropertyService = systemPropertyService;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) {
        pedestalStateService.timeoutPedestals(systemPropertyService.getPedestalHeartbeatTimeoutInMinutes());
    }
}
