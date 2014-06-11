package io.pillopl.dto.pedestallink.v1;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Link v1: one reading of the socket meter in Wh.
 */
public record MeterReading(LocalDateTime timestamp, Integer valueWh) implements Serializable {
}
