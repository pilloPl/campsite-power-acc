package io.pillopl.dto.map;

import java.io.Serializable;

/**
 * An outline on the map: CAMPSITE, SECTION (possibly the area of a power section) or PITCH, as a GeoJSON geometry
 * (longitude before latitude).
 */
public record MapAreaDTO(Long id, String areaType, String name, String pitchNo, Long powerSectionId, String geoJson)
        implements Serializable {
}
