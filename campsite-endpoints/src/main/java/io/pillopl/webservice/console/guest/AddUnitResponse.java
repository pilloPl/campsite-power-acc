package io.pillopl.webservice.console.guest;

import io.pillopl.dto.guest.UnitDTO;

import java.io.Serializable;

/**
 * The added unit.
 */
public record AddUnitResponse(UnitDTO unit) implements Serializable {
}
