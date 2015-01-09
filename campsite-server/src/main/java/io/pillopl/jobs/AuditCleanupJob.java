package io.pillopl.jobs;

import io.pillopl.audit.AuditService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Deletes the audit rows older than their expiry period.
 */
@DisallowConcurrentExecution
public class AuditCleanupJob extends QuartzJobBean {

    private final AuditService auditService;

    public AuditCleanupJob(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) {
        auditService.cleanAuditTable();
    }
}
