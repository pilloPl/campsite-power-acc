package io.pillopl.webservice.console.guest;

import io.pillopl.dto.guest.StayDTO;

import java.io.Serializable;

/**
 * The stay with the card's new validity.
 */
public record CheckInResponse(StayDTO stay) implements Serializable {
}
