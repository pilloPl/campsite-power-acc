package io.pillopl.webservice.console.map;

import io.pillopl.dto.map.MapDataDTO;

import java.io.Serializable;

/**
 * Campsites with outlines and pedestals, and the section load overlay.
 */
public record GetMapDataResponse(MapDataDTO map) implements Serializable {
}
