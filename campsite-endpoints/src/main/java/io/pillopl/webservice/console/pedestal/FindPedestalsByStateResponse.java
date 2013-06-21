package io.pillopl.webservice.console.pedestal;

import io.pillopl.dto.infrastructure.PedestalSummaryDTO;

import java.io.Serializable;
import java.util.List;

/**
 * One page of the pedestal list with the total count.
 */
public record FindPedestalsByStateResponse(List<PedestalSummaryDTO> pedestals, long total, Integer page,
                                           Integer rowsPerPage)
        implements Serializable {

    public FindPedestalsByStateResponse {
        pedestals = pedestals != null ? List.copyOf(pedestals) : List.of();
    }
}
