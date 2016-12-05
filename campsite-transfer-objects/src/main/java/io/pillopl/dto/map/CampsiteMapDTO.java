package io.pillopl.dto.map;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * A campsite (SITE group holding pedestals or map areas): the centre of its pedestals (null without coordinates),
 * its outlines (campsite, sections, pitches) and its pedestals.
 */
public record CampsiteMapDTO(Long groupId, String name, String displayName, BigDecimal latitude,
                             BigDecimal longitude, List<MapAreaDTO> areas, List<MapPedestalDTO> pedestals)
        implements Serializable {

    public CampsiteMapDTO {
        areas = areas != null ? List.copyOf(areas) : List.of();
        pedestals = pedestals != null ? List.copyOf(pedestals) : List.of();
    }
}
