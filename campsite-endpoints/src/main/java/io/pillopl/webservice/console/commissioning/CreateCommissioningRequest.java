package io.pillopl.webservice.console.commissioning;

import io.pillopl.dto.infrastructure.LocationDTO;

import java.io.Serializable;

/**
 * Prepares the commissioning of pedestals on the campsite of an affiliate owner.
 */
public record CreateCommissioningRequest(String authUsername, String ownerUsername, LocationDTO location,
                                         String comments, Boolean listedInApp, String accessMode)
        implements Serializable {
}
