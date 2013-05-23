package io.pillopl.dto.infrastructure;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A socket of a pedestal with its last meter reading and status.
 */
public record SocketDTO(Long id, String name, int position, Integer socketNo, String socketCode, String pitchNo,
                        String socketType, BigDecimal meterKwh, String status, LocalDateTime lastStatusUpdate,
                        boolean monitoring, boolean freeSocket) implements Serializable {
}
