package io.pillopl.dto.infrastructure;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Pedestal detail: type, group, address, flags, link state and sockets.
 */
public record PedestalDTO(Long id, String code, PedestalTypeDTO type, Long groupId, String groupName,
                          LocationDTO location, String serialNumber, String firmwareVersion, String modemIccid,
                          boolean billable, boolean settledByCampsite, boolean listedInApp,
                          boolean automatedWhitelistUpdates, boolean managedExternally, boolean remoteControl,
                          boolean deleted, LocalDateTime lastIncoming, boolean linkTimeout, List<SocketDTO> sockets)
        implements Serializable {

    public PedestalDTO {
        sockets = sockets != null ? List.copyOf(sockets) : List.of();
    }
}
