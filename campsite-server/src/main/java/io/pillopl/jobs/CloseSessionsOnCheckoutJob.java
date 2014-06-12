package io.pillopl.jobs;

import io.pillopl.supply.SupplyControlUnitImpl;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Closes the supply sessions the pedestals never stopped: on check-out of the stay, or after the timeout.
 */
@DisallowConcurrentExecution
public class CloseSessionsOnCheckoutJob extends QuartzJobBean {

    private final SupplyControlUnitImpl supplyControlUnit;

    public CloseSessionsOnCheckoutJob(SupplyControlUnitImpl supplyControlUnit) {
        this.supplyControlUnit = supplyControlUnit;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) {
        supplyControlUnit.closeStaleSessions();
    }
}
