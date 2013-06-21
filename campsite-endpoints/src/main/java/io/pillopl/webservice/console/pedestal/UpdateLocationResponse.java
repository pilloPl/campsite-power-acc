package io.pillopl.webservice.console.pedestal;

import io.pillopl.dto.infrastructure.PedestalDTO;

import java.io.Serializable;

/**
 * The pedestal with its new address.
 */
public record UpdateLocationResponse(PedestalDTO pedestal) implements Serializable {
}
