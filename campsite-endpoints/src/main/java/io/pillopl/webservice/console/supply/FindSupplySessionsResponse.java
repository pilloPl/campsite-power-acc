package io.pillopl.webservice.console.supply;

import io.pillopl.dto.supply.SupplySessionDTO;

import java.io.Serializable;
import java.util.List;

/**
 * One page of sessions, active first, then the newest; total is the number of all matching sessions.
 */
public record FindSupplySessionsResponse(List<SupplySessionDTO> sessions, long total, Integer page,
                                         Integer rowsPerPage) implements Serializable {

    public FindSupplySessionsResponse {
        sessions = sessions != null ? List.copyOf(sessions) : List.of();
    }
}
