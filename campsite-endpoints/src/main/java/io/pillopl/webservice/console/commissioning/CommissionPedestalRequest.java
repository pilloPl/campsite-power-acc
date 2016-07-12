package io.pillopl.webservice.console.commissioning;

import io.pillopl.dto.infrastructure.LocationDTO;

import java.io.Serializable;

/**
 * The installer mounts one pedestal directly, without a prepared commissioning.
 */
public record CommissionPedestalRequest(String pedestalCode, String ownerUsername,
                                        LocationDTO location, Boolean listedInApp, String accessMode)
        implements Serializable {
}
