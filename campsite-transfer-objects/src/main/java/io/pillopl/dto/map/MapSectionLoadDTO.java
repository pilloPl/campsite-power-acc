package io.pillopl.dto.map;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Load of a section switchboard: the current allowed to its active sessions against its capacity (share 0..1, null
 * without capacity).
 */
public record MapSectionLoadDTO(Long id, String sectionName, BigDecimal maxCurrentA, String algorithmBeanName,
                             long sockets, long activeSessions, BigDecimal allocatedCurrentA, BigDecimal loadShare)
        implements Serializable {
}
