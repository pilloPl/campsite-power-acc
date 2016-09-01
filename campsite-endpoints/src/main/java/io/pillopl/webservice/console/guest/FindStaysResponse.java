package io.pillopl.webservice.console.guest;

import io.pillopl.dto.guest.StayDTO;

import java.io.Serializable;
import java.util.List;

/**
 * The stays, latest arrival first.
 */
public record FindStaysResponse(List<StayDTO> stays) implements Serializable {

    public FindStaysResponse {
        stays = stays != null ? List.copyOf(stays) : List.of();
    }
}
