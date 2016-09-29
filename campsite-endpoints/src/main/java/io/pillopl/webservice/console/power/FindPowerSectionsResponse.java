package io.pillopl.webservice.console.power;

import io.pillopl.dto.power.PowerSectionDTO;

import java.io.Serializable;
import java.util.List;

/**
 * The sections by name with their sockets and allocation, and the allocation algorithms a section may select.
 */
public record FindPowerSectionsResponse(List<PowerSectionDTO> sections, List<String> availableAlgorithms)
        implements Serializable {

    public FindPowerSectionsResponse {
        sections = sections != null ? List.copyOf(sections) : List.of();
        availableAlgorithms = availableAlgorithms != null ? List.copyOf(availableAlgorithms) : List.of();
    }
}
