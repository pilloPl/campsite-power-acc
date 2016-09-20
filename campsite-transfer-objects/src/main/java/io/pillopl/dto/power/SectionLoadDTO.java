package io.pillopl.dto.power;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Load of a section per hour against its capacity: the rated current held by the sessions running in the hour
 * (what the allocation algorithm counts) and the sum of the highest currents the sockets measured in the hour.
 */
public record SectionLoadDTO(Long sectionId, String sectionName, BigDecimal maxCurrentA, List<Point> points)
        implements Serializable {

    public SectionLoadDTO {
        points = points != null ? List.copyOf(points) : List.of();
    }

    public record Point(LocalDateTime hour, int allocatedA, BigDecimal measuredA, int sessions)
            implements Serializable {
    }
}
