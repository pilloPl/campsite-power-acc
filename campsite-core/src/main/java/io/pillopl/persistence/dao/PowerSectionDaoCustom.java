package io.pillopl.persistence.dao;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Load of a section over time, hand-written SQL from the SQL map.
 */
public interface PowerSectionDaoCustom {

    /**
     * One row per hour starting in [from, to): rated current of the sessions running in the hour, the sum of the
     * sockets' highest measured currents and the number of sessions.
     */
    List<SectionLoadRow> findSectionLoadPerHour(Long sectionId, LocalDateTime from, LocalDateTime to);

    record SectionLoadRow(LocalDateTime hour, int allocatedA, BigDecimal measuredA, int sessions) {
    }
}
