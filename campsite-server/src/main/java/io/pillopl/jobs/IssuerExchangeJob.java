package io.pillopl.jobs;

import io.pillopl.issuer.IssuerExchangeService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Exchanges card lists and consumption records with the issuer partners (status per partner and direction).
 */
@DisallowConcurrentExecution
public class IssuerExchangeJob extends QuartzJobBean {

    private final IssuerExchangeService issuerExchangeService;

    public IssuerExchangeJob(IssuerExchangeService issuerExchangeService) {
        this.issuerExchangeService = issuerExchangeService;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) {
        issuerExchangeService.exchangeAll();
    }
}
