package io.pillopl.jobs;

import io.pillopl.user.UserinfoSummaryService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Refreshes the materialized view USERINFO_SUMMARY of the self-service user lists.
 */
@DisallowConcurrentExecution
public class UserinfoSummaryRefreshJob extends QuartzJobBean {

    private final UserinfoSummaryService userinfoSummaryService;

    public UserinfoSummaryRefreshJob(UserinfoSummaryService userinfoSummaryService) {
        this.userinfoSummaryService = userinfoSummaryService;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) {
        userinfoSummaryService.refresh();
    }
}
