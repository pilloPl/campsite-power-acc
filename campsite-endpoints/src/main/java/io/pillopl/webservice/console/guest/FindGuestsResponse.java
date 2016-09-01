package io.pillopl.webservice.console.guest;

import io.pillopl.dto.guest.GuestDTO;

import java.io.Serializable;
import java.util.List;

/**
 * The guests found, by name.
 */
public record FindGuestsResponse(List<GuestDTO> guests) implements Serializable {

    public FindGuestsResponse {
        guests = guests != null ? List.copyOf(guests) : List.of();
    }
}
