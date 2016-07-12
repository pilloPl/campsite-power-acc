package io.pillopl.webservice.console.card;

import java.io.Serializable;

/**
 * Cards by the UID from the reader or the printed number.
 */
public record FindCardsRequest(String rfidOrNumber) implements Serializable {
}
