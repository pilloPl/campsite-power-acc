package io.pillopl.dto.map;

import java.io.Serializable;

/**
 * A socket in the popup of its pedestal: status, pitch, section, and the active session the popup can stop.
 */
public record MapSocketDTO(Long id, Integer socketNo, String name, String socketCode, String pitchNo, String status,
                           Long powerSectionId, Long activeSessionId, Integer sessionMaxCurrentA)
        implements Serializable {
}
