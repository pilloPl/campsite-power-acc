package io.pillopl.jobs;

import io.pillopl.card.LocalCardListDistributionService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Sends the full local card lists to the pedestals (errors are mailed by the distribution service).
 */
@DisallowConcurrentExecution
public class LocalCardListDistributionJob extends QuartzJobBean {

    private final LocalCardListDistributionService localCardListDistributionService;

    public LocalCardListDistributionJob(LocalCardListDistributionService localCardListDistributionService) {
        this.localCardListDistributionService = localCardListDistributionService;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) {
        localCardListDistributionService.distribute();
    }
}
