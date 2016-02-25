package io.pillopl.jobs;

import io.pillopl.erp.ErpExchangeService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Hands the orders of new guests to the ERP for partner and account numbers.
 */
@DisallowConcurrentExecution
public class ErpAccountExportJob extends QuartzJobBean {

    private final ErpExchangeService erpExchangeService;

    public ErpAccountExportJob(ErpExchangeService erpExchangeService) {
        this.erpExchangeService = erpExchangeService;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) {
        erpExchangeService.exportPartnerRequests();
    }
}
