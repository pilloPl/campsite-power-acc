package io.pillopl.webservice.console.card;

import java.io.Serializable;

/**
 * Dry run of the pedestal's question for a card at a socket.
 */
public record CheckCardAtSocketRequest(String cardRfid, String pedestalCode, Integer socketNo) implements Serializable {
}
