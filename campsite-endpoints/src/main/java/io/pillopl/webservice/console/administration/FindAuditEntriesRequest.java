package io.pillopl.webservice.console.administration;

import java.io.Serializable;

/**
 * Audit search; dates as ISO date or date-time, a date-only end includes that day.
 */
public record FindAuditEntriesRequest(String name, String type, String status, String category,
                                      String dateFrom, String dateTo, String freeSearch, String sortColumn,
                                      String sortOrder)
        implements Serializable {
}
