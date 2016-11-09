package io.pillopl.webservice.console.supply;

import java.io.Serializable;

/**
 * Marks an incident reviewed with what the service team found.
 */
public record ReviewSupplyIncidentRequest(Long incidentId, String comment) implements Serializable {
}
