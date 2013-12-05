package io.pillopl.dto.pricing;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * When a tariff element applies: hours, season dates, days of the week (MONDAY..SUNDAY), kWh range of the supply.
 */
public record TariffRestrictionDTO(LocalTime startTime, LocalTime endTime, LocalDate startDate, LocalDate endDate,
                                   List<String> daysOfWeek, BigDecimal minKwh, BigDecimal maxKwh)
        implements Serializable {
}
