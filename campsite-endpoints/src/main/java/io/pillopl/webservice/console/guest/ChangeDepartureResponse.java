package io.pillopl.webservice.console.guest;

import io.pillopl.dto.guest.StayDTO;

import java.io.Serializable;

/**
 * The stay with its new departure and card validity.
 */
public record ChangeDepartureResponse(StayDTO stay) implements Serializable {
}
