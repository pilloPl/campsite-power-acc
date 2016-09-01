package io.pillopl.webservice.console.guest;

import io.pillopl.dto.guest.OrderDTO;

import java.io.Serializable;

/**
 * The cancelled order.
 */
public record CancelOrderResponse(OrderDTO order) implements Serializable {
}
