package io.pillopl.dto.pedestallink.v1;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Link v1: the socket started to supply for the card. The meter value is in Wh, the timestamp is the controller's
 * clock (1970 after a power loss).
 */
public record SupplyStartedRequest(Integer socketNo, String authTag, Integer meterStartWh, LocalDateTime timestamp)
        implements Serializable {
}
