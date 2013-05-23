package io.pillopl.dto.infrastructure;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record ReceptionHoursDTO(DayOfWeek dayOfWeek, LocalTime openFrom, LocalTime openTo) implements Serializable {
}
