package io.pillopl.webservice.console.guest;

import io.pillopl.dto.guest.StayDTO;

import java.io.Serializable;

/**
 * The checked-out stay; its open supply sessions are closed by the stale-session job after the configured delay.
 */
public record CheckOutResponse(StayDTO stay) implements Serializable {
}
