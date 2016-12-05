package io.pillopl.dto.map;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A pedestal on the map at the coordinates of its location, coloured by the statuses of its sockets; timeout marks
 * a pedestal whose link timed out.
 */
public record MapPedestalDTO(Long id, String code, BigDecimal latitude, BigDecimal longitude, String pedestalType,
                             String linkVersion, boolean remoteControl, boolean timeout, LocalDateTime lastIncoming,
                             List<MapSocketDTO> sockets) implements Serializable {

    public MapPedestalDTO {
        sockets = sockets != null ? List.copyOf(sockets) : List.of();
    }
}
