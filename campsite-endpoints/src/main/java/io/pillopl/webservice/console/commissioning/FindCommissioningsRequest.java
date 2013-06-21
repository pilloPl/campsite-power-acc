package io.pillopl.webservice.console.commissioning;

import java.io.Serializable;

/**
 * Open commissionings of the owners in the calling user's STAFF scope.
 */
public record FindCommissioningsRequest(String authUsername) implements Serializable {
}
