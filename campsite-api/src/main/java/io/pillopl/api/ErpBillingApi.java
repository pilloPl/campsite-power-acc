package io.pillopl.api;

import io.pillopl.dto.consumption.ConsumptionRecordDTO;
import io.pillopl.dto.erp.ErpBillingLineDTO;
import io.pillopl.dto.erp.ErpBillingRunDTO;
import io.pillopl.value.Username;

import java.util.List;

/**
 * ERP billing tab (the network's own billing accounts, no campsite scope): runs, lines, preview, run, retry of
 * refused lines and the billable records no export takes. REPORT_VIEW reads, BILLING_EDIT runs and retries.
 */
public interface ErpBillingApi {

    /**
     * Every billing run, newest period first.
     */
    List<ErpBillingRunDTO> findErpBillingRuns(Username authUsername);

    /**
     * @param billingPeriod "2026_07"
     * @param exportStatus  null for all lines
     */
    List<ErpBillingLineDTO> findErpBillingLines(Username authUsername, String billingPeriod, String exportStatus);

    /**
     * The lines a run of the month would create now; nothing is stored.
     */
    List<ErpBillingLineDTO> previewErpBilling(Username authUsername, int month, int year);

    /**
     * Runs the billing of the month (the job runs the last month on its day).
     *
     * @throws io.pillopl.api.exception.ConflictException when the month is billed already
     */
    List<ErpBillingLineDTO> runErpBilling(Username authUsername, int month, int year);

    /**
     * The refused lines of the period go to the ERP again with the next file.
     *
     * @return the number of lines sent again
     */
    int retryErpExport(Username authUsername, String billingPeriod);

    List<ConsumptionRecordDTO> findRecordsWithoutExport(Username authUsername);
}
