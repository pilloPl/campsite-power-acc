package io.pillopl.dto.map;

import java.io.Serializable;
import java.util.List;

/**
 * Everything the map tab draws for the caller's campsites: the campsites with their outlines and pedestals, and the
 * load of the section switchboards for the overlay.
 */
public record MapDataDTO(List<CampsiteMapDTO> campsites, List<SectionLoadDTO> sections) implements Serializable {

    public MapDataDTO {
        campsites = campsites != null ? List.copyOf(campsites) : List.of();
        sections = sections != null ? List.copyOf(sections) : List.of();
    }
}
