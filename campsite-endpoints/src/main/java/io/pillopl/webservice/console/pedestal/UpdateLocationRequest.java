package io.pillopl.webservice.console.pedestal;

import io.pillopl.dto.infrastructure.LocationDTO;

import java.io.Serializable;

/**
 * New address of a pedestal of an affiliate owner.
 */
public record UpdateLocationRequest(String authUsername, String ownerUsername, String pedestalCode,
                                    LocationDTO location)
        implements Serializable {
}
