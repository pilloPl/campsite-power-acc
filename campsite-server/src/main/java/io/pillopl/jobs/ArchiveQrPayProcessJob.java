package io.pillopl.jobs;

import io.pillopl.qrpay.QrPayProcessService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Moves the finished QR payment chains to the archive.
 */
@DisallowConcurrentExecution
public class ArchiveQrPayProcessJob extends QuartzJobBean {

    private final QrPayProcessService qrPayProcessService;

    public ArchiveQrPayProcessJob(QrPayProcessService qrPayProcessService) {
        this.qrPayProcessService = qrPayProcessService;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) {
        qrPayProcessService.archiveFinishedProcesses();
    }
}
