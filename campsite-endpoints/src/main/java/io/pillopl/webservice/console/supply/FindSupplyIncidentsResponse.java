package io.pillopl.webservice.console.supply;

import io.pillopl.dto.supply.SupplyIncidentDTO;

import java.io.Serializable;
import java.util.List;

/**
 * One page of incidents, newest first.
 */
public record FindSupplyIncidentsResponse(List<SupplyIncidentDTO> incidents, long total, Integer page,
                                          Integer rowsPerPage) implements Serializable {

    public FindSupplyIncidentsResponse {
        incidents = incidents != null ? List.copyOf(incidents) : List.of();
    }
}
