package io.pillopl.webservice.console.consumption;

import io.pillopl.dto.consumption.ConsumptionRecordDTO;

import java.io.Serializable;

/**
 * The reviewed record with its new validation state, price and export status.
 */
public record ReviewConsumptionRecordResponse(ConsumptionRecordDTO consumptionRecord) implements Serializable {
}
