package io.pillopl.webservice.console.erp;

import java.io.Serializable;

/**
 * Billable consumption records no export takes (older than RecordWithoutExportAfterDays).
 */
public record FindRecordsWithoutExportRequest() implements Serializable {
}
