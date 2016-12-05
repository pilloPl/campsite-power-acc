package io.pillopl.dto.monitoring;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A pedestal whose link timed out: no message since {@code lastIncoming}, timed out at {@code lastTimeout}.
 */
public record HeartbeatTimeoutDTO(Long pedestalId, String pedestalCode, Long campsiteGroupId, String campsiteName,
                                  String linkVersion, LocalDateTime lastIncoming, LocalDateTime lastTimeout)
        implements Serializable {
}
