package io.pillopl.supply;

import java.time.LocalDateTime;

/**
 * Plug-in and plug-out of a supply and the duration in seconds (null when the plug-in is unknown).
 */
public record SupplyDuration(LocalDateTime plugIn, LocalDateTime plugOut, Long durationInSeconds) {

    public boolean isNegative() {
        return durationInSeconds != null && durationInSeconds < 0;
    }
}
