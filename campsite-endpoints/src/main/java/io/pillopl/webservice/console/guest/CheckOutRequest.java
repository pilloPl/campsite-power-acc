package io.pillopl.webservice.console.guest;

import java.io.Serializable;

/**
 * Check-out of a stay.
 */
public record CheckOutRequest(String authUsername, Long stayId) implements Serializable {
}
