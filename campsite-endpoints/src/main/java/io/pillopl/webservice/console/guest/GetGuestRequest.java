package io.pillopl.webservice.console.guest;

import java.io.Serializable;

/**
 * The page of one guest.
 */
public record GetGuestRequest(Long guestId) implements Serializable {
}
