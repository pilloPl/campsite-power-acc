package io.pillopl.webservice.console.card;

import java.io.Serializable;

/**
 * Whether the list was sent (false: the pedestal keeps no local list).
 */
public record SendLocalCardListResponse(String pedestalCode, boolean sent) implements Serializable {
}
