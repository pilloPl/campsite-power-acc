package io.pillopl.webservice.console.supply;

import io.pillopl.dto.supply.SupplyIncidentDTO;

import java.io.Serializable;

/**
 * The reviewed incident.
 */
public record ReviewSupplyIncidentResponse(SupplyIncidentDTO incident) implements Serializable {
}
