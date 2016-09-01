package io.pillopl.webservice.console.consumption;

import java.io.Serializable;

/**
 * Prices a record again with the network tariff (failed pricing or waiting for export).
 */
public record RepriceConsumptionRecordRequest(Long consumptionRecordId) implements Serializable {
}
