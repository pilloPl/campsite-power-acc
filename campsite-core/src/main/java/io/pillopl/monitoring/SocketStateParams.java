package io.pillopl.monitoring;

import io.pillopl.infrastructure.SocketStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * What one pedestal message tells about a socket. Everything but the timestamp is optional: a meter reading carries
 * no status, a status message no meter reading, only some controllers send error flags, current or firmware.
 */
public record SocketStateParams(LocalDateTime timestamp, SocketStatus status, Long errorFlags, BigDecimal meterKwh,
                                BigDecimal currentA, String firmwareVersion) {

    public static SocketStateParams status(LocalDateTime timestamp, SocketStatus status, Long errorFlags) {
        return new SocketStateParams(timestamp, status, errorFlags, null, null, null);
    }

    public static SocketStateParams meterReading(LocalDateTime timestamp, BigDecimal meterKwh) {
        return new SocketStateParams(timestamp, null, null, meterKwh, null, null);
    }

    public void validate() {
        if (timestamp == null) {
            throw new AssertionError("timestamp == null");
        }
    }
}
