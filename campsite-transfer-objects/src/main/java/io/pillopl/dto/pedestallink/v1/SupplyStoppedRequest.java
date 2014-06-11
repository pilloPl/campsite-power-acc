package io.pillopl.dto.pedestallink.v1;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Link v1: the socket stopped supplying. The card may be another card than the one that started; the meter value
 * is in Wh; the reason is one of the stop reasons (LOCAL when missing).
 */
public record SupplyStoppedRequest(Integer transactionId, String authTag, Integer meterStopWh, LocalDateTime timestamp,
                                   String reason) implements Serializable {
}
