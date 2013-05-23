package io.pillopl.dto.infrastructure;

import java.io.Serializable;

/**
 * One row of the pedestal list.
 */
public record PedestalSummaryDTO(Long id, String code, String type, String groupName, String city,
                                 int numberOfSockets, boolean socketInError, boolean deleted) implements Serializable {
}
