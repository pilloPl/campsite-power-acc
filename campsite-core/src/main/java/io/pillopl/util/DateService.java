package io.pillopl.util;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * The system time of the backend (pedestals send their own remote dates; this is the time of the pedestal backend).
 * Tests may set a fixed clock.
 */
@Service
public class DateService {

    private volatile Clock clock = Clock.systemDefaultZone();

    public LocalDateTime getNow() {
        return LocalDateTime.now(clock);
    }

    public LocalDate getToday() {
        return LocalDate.now(clock);
    }

    public LocalDateTime getNowInUtc() {
        return LocalDateTime.now(clock.withZone(ZoneOffset.UTC));
    }

    public void setClock(Clock clock) {
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
    }
}
