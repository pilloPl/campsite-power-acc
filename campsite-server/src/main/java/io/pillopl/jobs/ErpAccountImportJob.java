package io.pillopl.jobs;

import io.pillopl.erp.ErpExchangeService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Takes the partner and account numbers (and their corrections) the ERP answered.
 */
@DisallowConcurrentExecution
public class ErpAccountImportJob extends QuartzJobBean {

    private final ErpExchangeService erpExchangeService;

    public ErpAccountImportJob(ErpExchangeService erpExchangeService) {
        this.erpExchangeService = erpExchangeService;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) {
        erpExchangeService.importPartnerAnswers();
    }
}
