package io.pillopl.persistence.dao;

import io.pillopl.consumption.ConsumptionRecord;
import io.pillopl.consumption.ExportStatus;
import io.pillopl.consumption.RecordValidationState;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Consumption records seen by a user: the operator side (pedestals in the subtree of the user's SITE group) UNION
 * the issuer side (guests in the subtree of the user's ISSUER group). Three variants of the same SQL: all records,
 * one page of the console list, the ids waiting for export.
 */
public interface ConsumptionRecordDaoCustom {

    /**
     * @param siteGroupId   SITE group of the user, null when the user has none (that side is empty)
     * @param issuerGroupId ISSUER group of the user, null when the user has none (that side is empty)
     * @return the records with plug-out in [from, to], newest first
     */
    List<ConsumptionRecord> findConsumptionRecords(LocalDateTime from, LocalDateTime to, Long siteGroupId,
                                                   Long issuerGroupId);

    /**
     * @param validationState null for every state
     * @param pageNumber      first page is 1
     */
    List<ConsumptionRecord> findConsumptionRecords(LocalDateTime from, LocalDateTime to, Long siteGroupId,
                                                   Long issuerGroupId, RecordValidationState validationState,
                                                   int pageNumber, int pageSize);

    /**
     * Ids of the records waiting for export (EXPORT_STATUS NEW), oldest first.
     */
    List<Long> findConsumptionRecordIdsForExport(LocalDateTime from, LocalDateTime to, Long siteGroupId,
                                                 Long issuerGroupId);

    /**
     * Next number of the natural record id, from CONSUMPTION_RECORD_NO_SEQ.
     */
    long nextRecordNumber();

    /**
     * Consumption of a billing account of the network in a month of plug-out, for the ERP billing.
     *
     * @param tariffId     tariff the records were priced with (the contract's tariff marks supplies on its pitch)
     * @param billed       billed by an earlier run (with a billing period); false for the records not billed yet
     * @param supplyPeriod month of plug-out, "2026_07"
     */
    record ErpConsumption(String billingAccountNo, Long tariffId, boolean billed, String supplyPeriod,
                          BigDecimal kwh, BigDecimal cost, long records) {
    }

    /**
     * @param to exclusive
     * @return the not billed (EXPORT_STATUS NEW) and the billed records of the network's billing accounts with
     * plug-out in [from, to), per account, tariff, billed or not and month of plug-out
     */
    List<ErpConsumption> findErpConsumptionPerMonth(LocalDateTime from, LocalDateTime to);

    /**
     * The not billed records of the given consumption (account, tariff, month of plug-out) with plug-out in
     * [from, to) are billed in the period (EXPORT_STATUS WORK until the ERP file is written).
     *
     * @param billedConsumption the consumption that went into ERP lines
     * @return number of records marked
     */
    int markRecordsForErpBilling(Collection<ErpConsumption> billedConsumption, LocalDateTime from, LocalDateTime to,
                                 String period);

    /**
     * The records billed in the period follow their ERP lines.
     *
     * @param billingAccountNo null for every account of the period
     * @return number of records moved
     */
    int updateErpBilledRecords(String period, String billingAccountNo, Collection<ExportStatus> fromStatuses,
                               ExportStatus exportStatus);
}
