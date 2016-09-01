package io.pillopl.webservice.console.guest;

import java.io.Serializable;

/**
 * Guests of the caller's issuers whose name, first name, plate or card number contains the text (all for none).
 */
public record FindGuestsRequest(String search) implements Serializable {
}
