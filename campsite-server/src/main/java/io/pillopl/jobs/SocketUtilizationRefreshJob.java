package io.pillopl.jobs;

import io.pillopl.monitoring.PedestalStateService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Refreshes the materialized view SOCKET_UTILIZATION of the occupancy reports.
 */
@DisallowConcurrentExecution
public class SocketUtilizationRefreshJob extends QuartzJobBean {

    private final PedestalStateService pedestalStateService;

    public SocketUtilizationRefreshJob(PedestalStateService pedestalStateService) {
        this.pedestalStateService = pedestalStateService;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) {
        pedestalStateService.refreshSocketUtilization();
    }
}
