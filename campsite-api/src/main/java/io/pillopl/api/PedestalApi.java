package io.pillopl.api;

import io.pillopl.commons.ListPage;
import io.pillopl.commons.Pagination;
import io.pillopl.dto.infrastructure.LocationDTO;
import io.pillopl.dto.infrastructure.PedestalDTO;
import io.pillopl.dto.infrastructure.PedestalSummaryDTO;
import io.pillopl.dto.infrastructure.SocketDTO;
import io.pillopl.value.PedestalCode;
import io.pillopl.value.PitchNo;
import io.pillopl.value.SocketCode;
import io.pillopl.value.SocketId;
import io.pillopl.value.Username;

/**
 * Pedestals and sockets of the SITE scope of the calling user.
 */
public interface PedestalApi {

    /**
     * @param state      null for all pedestals, otherwise a pedestal list state
     * @param pagination null for the whole list
     */
    ListPage<PedestalSummaryDTO> findPedestalsByState(Username authUsername, String state, Pagination pagination);

    PedestalDTO getPedestal(Username authUsername, PedestalCode code);

    PedestalDTO findPedestalBySocketCode(Username authUsername, SocketCode socketCode);

    void deletePedestal(Username authUsername, Username ownerUsername, PedestalCode code);

    PedestalDTO updateLocation(Username authUsername, Username ownerUsername, PedestalCode code, LocationDTO location);

    /**
     * @param pitchNo null removes the assignment
     */
    SocketDTO assignPitch(Username authUsername, SocketId socketId, PitchNo pitchNo);

    SocketDTO setFreeSocket(Username authUsername, SocketId socketId, boolean freeSocket);
}
