package io.pillopl.jobs;

import io.pillopl.erp.ErpExchangeService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Bills the month before to the ERP (once), writes the billing lines not sent yet and takes the ERP's booking
 * answers.
 */
@DisallowConcurrentExecution
public class ErpBillingExportJob extends QuartzJobBean {

    private final ErpExchangeService erpExchangeService;

    public ErpBillingExportJob(ErpExchangeService erpExchangeService) {
        this.erpExchangeService = erpExchangeService;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) {
        erpExchangeService.exportBillingLines();
        erpExchangeService.importBillingAnswers();
    }
}
