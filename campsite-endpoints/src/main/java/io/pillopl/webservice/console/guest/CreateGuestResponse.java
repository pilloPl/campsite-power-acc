package io.pillopl.webservice.console.guest;

import io.pillopl.dto.guest.GuestDTO;

import java.io.Serializable;

/**
 * The created guest.
 */
public record CreateGuestResponse(GuestDTO guest) implements Serializable {
}
