package io.pillopl.api;

import io.pillopl.commons.DateRange;
import io.pillopl.commons.Pagination;
import io.pillopl.dto.consumption.ConsumptionRecordDTO;
import io.pillopl.value.Username;

import java.util.List;

/**
 * Consumption records seen by a user (his campsites' pedestals and his issuer's guests) and the review of the
 * records waiting in the review queue.
 */
public interface ConsumptionRecordApi {

    /**
     * @param period          plug-out period, both ends required
     * @param validationState VALID, SUSPECT, CONFIRMED, REJECTED or null for all
     * @param pagination      null for all records of the period
     */
    List<ConsumptionRecordDTO> findConsumptionRecords(Username authUsername, DateRange period,
                                                      String validationState, Pagination pagination);

    /**
     * Confirms (priced and exported then) or rejects (never exported) a record of the review queue.
     */
    ConsumptionRecordDTO reviewConsumptionRecord(Username authUsername, Long recordId, boolean confirmed,
                                                 String comment);

    /**
     * Prices a record again with the network tariff (after a tariff fix): records whose pricing failed or that wait
     * for their export, never suspect, rejected, exported or imported ones. CONSUMPTION_RECORD_REVIEW.
     */
    ConsumptionRecordDTO repriceConsumptionRecord(Username authUsername, Long recordId);

    /**
     * Puts a record whose export failed (ERROR) back to NEW, so that the next export takes it. BILLING_EDIT.
     */
    ConsumptionRecordDTO requeueConsumptionRecordExport(Username authUsername, Long recordId);
}
