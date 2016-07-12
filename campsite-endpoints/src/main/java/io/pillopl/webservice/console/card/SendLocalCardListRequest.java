package io.pillopl.webservice.console.card;

import java.io.Serializable;

/**
 * Sends the whole local card list to a pedestal now.
 */
public record SendLocalCardListRequest(String pedestalCode) implements Serializable {
}
