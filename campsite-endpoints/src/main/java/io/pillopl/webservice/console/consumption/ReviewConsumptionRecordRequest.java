package io.pillopl.webservice.console.consumption;

import java.io.Serializable;

/**
 * Confirm (true) or reject (false) a record of the review queue, with an optional comment.
 */
public record ReviewConsumptionRecordRequest(Long consumptionRecordId, Boolean confirmed,
                                             String comment) implements Serializable {
}
