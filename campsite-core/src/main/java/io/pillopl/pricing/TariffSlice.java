package io.pillopl.pricing;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Part of a supply inside which no restriction of the tariff changes (no midnight, no window start or end).
 * Every element applies to the whole slice or not at all, so the slice start decides.
 */
public record TariffSlice(LocalDateTime start, LocalDateTime end) {

    public long seconds() {
        return Duration.between(start, end).getSeconds();
    }
}
