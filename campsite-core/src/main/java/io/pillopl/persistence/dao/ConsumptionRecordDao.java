package io.pillopl.persistence.dao;

import io.pillopl.consumption.ConsumptionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Consumption records; the visibility queries of both sides are hand-written SQL in {@link ConsumptionRecordDaoCustom}.
 */
public interface ConsumptionRecordDao extends JpaRepository<ConsumptionRecord, Long>, ConsumptionRecordDaoCustom {

    ConsumptionRecord findBySupplySessionId(Long supplySessionId);

    ConsumptionRecord findByRecordNo(String recordNo);

    /**
     * A record imported from an issuer partner, by the partner's name and record number.
     */
    ConsumptionRecord findBySourceNameAndExternalRecordNo(String sourceName, String externalRecordNo);

    /**
     * Priced, billable card records the network bills itself that still wait for an export (NEW, no ERP period)
     * although their plug-out is older than the given time, oldest first.
     */
    @Query("select r from ConsumptionRecord r where r.exportStatus = io.pillopl.consumption.ExportStatus.NEW"
            + " and r.paymentChannel = io.pillopl.consumption.PaymentChannel.CARD"
            + " and r.settlementFlags.billable = true and r.settlementFlags.settledByCampsite = false"
            + " and r.pricingStatus = io.pillopl.pricing.PricingStatus.PRICED"
            + " and r.plugOut < :plugOutBefore order by r.plugOut, r.id")
    List<ConsumptionRecord> findBillableRecordsWaitingForExportSince(
            @Param("plugOutBefore") LocalDateTime plugOutBefore);
}
