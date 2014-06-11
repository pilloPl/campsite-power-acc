package io.pillopl.dto.pedestallink.v1;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Link v1: answer to a boot. ACCEPTED for a known pedestal, REJECTED otherwise; the controller sets its clock and
 * sends heartbeats in the given interval (seconds).
 */
public record BootResponse(String status, LocalDateTime currentTime, int heartbeatInterval) implements Serializable {
}
