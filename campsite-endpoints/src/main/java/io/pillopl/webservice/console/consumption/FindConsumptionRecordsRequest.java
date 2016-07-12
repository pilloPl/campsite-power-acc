package io.pillopl.webservice.console.consumption;

import java.io.Serializable;

/**
 * Consumption records with plug-out in [from, to] (dates "2026-07-01" or date-times), optionally of one validation
 * state, optionally one page (page from 0).
 */
public record FindConsumptionRecordsRequest(String from, String to, String validationState,
                                            Integer page, Integer rowsPerPage) implements Serializable {
}
