package io.pillopl.persistence.dao;

import io.pillopl.consumption.ConsumptionRecord;
import io.pillopl.consumption.RecordValidationState;

import java.time.LocalDateTime;
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
}
