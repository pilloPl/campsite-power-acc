package io.pillopl.api;

import io.pillopl.dto.map.MapDataDTO;
import io.pillopl.value.Username;

/**
 * The map of the caller's campsites (PEDESTAL_VIEW). The remote supply start and stop of the popup are the calls of
 * {@link SupplyApi}.
 */
public interface MapApi {

    /**
     * @param campsiteGroupId a SITE group of the caller's subtree, null for the whole scope
     */
    MapDataDTO getMapData(Username authUsername, Long campsiteGroupId);
}
