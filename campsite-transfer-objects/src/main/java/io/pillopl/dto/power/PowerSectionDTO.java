package io.pillopl.dto.power;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * A section switchboard with its capacity, the bean of its allocation algorithm (algorithmAvailable false: the
 * section refuses every supply until a valid algorithm is selected), the sockets hanging on it and what the active
 * sessions hold of the capacity now.
 */
public record PowerSectionDTO(Long id, String sectionName, BigDecimal maxCurrentA, String algorithmBeanName,
                              boolean algorithmAvailable, int socketCount, int allocatedA, int activeSessions,
                              List<SocketAllocation> sockets) implements Serializable {

    public PowerSectionDTO {
        sockets = sockets != null ? List.copyOf(sockets) : List.of();
    }

    /**
     * A socket of the section: its rated current and the current its active session holds (0 when idle).
     */
    public record SocketAllocation(Long socketId, String socketCode, String pedestalCode, Integer socketNo,
                                   String pitchNo, String status, int ratedCurrentA, int allocatedA)
            implements Serializable {
    }
}
