package io.pillopl.webservice.console.consumption;

import java.io.Serializable;

/**
 * A record whose export failed (ERROR) waits for the next export again.
 */
public record RequeueConsumptionRecordExportRequest(Long consumptionRecordId) implements Serializable {
}
