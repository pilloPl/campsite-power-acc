package io.pillopl.dto.admin;

import io.pillopl.commons.DateRange;

import java.io.Serializable;

/**
 * Filter of the audit search; blank fields do not filter.
 */
public record AuditFilterDTO(String name, String type, String status, String category, DateRange period,
                             String freeSearch) implements Serializable {

    public AuditFilterDTO {
        period = period != null ? period : DateRange.UNBOUNDED;
    }
}
