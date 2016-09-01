package io.pillopl.webservice.console.guest;

import java.io.Serializable;

/**
 * Cancels an order (booking) not active yet, with an optional reason.
 */
public record CancelOrderRequest(Long orderId, String reason) implements Serializable {
}
