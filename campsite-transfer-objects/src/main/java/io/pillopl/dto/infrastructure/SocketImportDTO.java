package io.pillopl.dto.infrastructure;

import java.io.Serializable;

/**
 * A socket of a pedestal in a supplier delivery.
 */
public record SocketImportDTO(String externalId, Integer position, String name, String plugStandard, Integer phases,
                              Integer amperes, String pitchNo) implements Serializable {
}
