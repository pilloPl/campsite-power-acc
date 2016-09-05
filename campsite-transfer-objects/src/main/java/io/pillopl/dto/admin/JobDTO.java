package io.pillopl.dto.admin;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A scheduled job: interval of its trigger (from SYSTEM_PROPERTY at start-up), trigger state (NORMAL, PAUSED,
 * BLOCKED, ...), last and next run, whether it runs right now, and whether the scheduler runs at all (STARTED) or
 * waits (STANDBY: triggered runs wait for the start).
 */
public record JobDTO(String name, String description, Long intervalSeconds, String state,
                     LocalDateTime previousFireTime, LocalDateTime nextFireTime, boolean running,
                     String schedulerState) implements Serializable {
}
