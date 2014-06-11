package io.pillopl.dto.pedestallink.v1;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Link v1: the pedestal backend received a heartbeat, socket status or meter readings; carries its time so the
 * controller can correct its clock.
 */
public record AcknowledgeResponse(LocalDateTime currentTime) implements Serializable {
}
