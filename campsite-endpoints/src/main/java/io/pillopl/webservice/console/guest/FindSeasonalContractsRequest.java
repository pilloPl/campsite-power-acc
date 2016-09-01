package io.pillopl.webservice.console.guest;

import java.io.Serializable;

/**
 * Seasonal contracts of the caller's campsites and guests; status ACTIVE, TERMINATION_REQUESTED,
 * ENDED or empty for all.
 */
public record FindSeasonalContractsRequest(String status) implements Serializable {
}
