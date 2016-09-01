package io.pillopl.webservice.console.guest;

import io.pillopl.dto.guest.GuestDetailDTO;

import java.io.Serializable;

/**
 * The guest with units, cards, billing accounts, seasonal contracts, stays and orders.
 */
public record GetGuestResponse(GuestDetailDTO guest) implements Serializable {
}
