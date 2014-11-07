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
}
