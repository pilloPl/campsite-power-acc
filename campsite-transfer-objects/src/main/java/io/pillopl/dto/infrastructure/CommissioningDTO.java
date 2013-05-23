package io.pillopl.dto.infrastructure;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * An open commissioning of pedestals on an affiliate campsite.
 */
public record CommissioningDTO(Long id, String ownerUsername, String ownerName, LocationDTO location, String comments,
                               boolean listedInApp, String accessMode, String token, LocalDateTime createDate)
        implements Serializable {
}
