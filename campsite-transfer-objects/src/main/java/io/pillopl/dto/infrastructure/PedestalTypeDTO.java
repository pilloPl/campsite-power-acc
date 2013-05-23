package io.pillopl.dto.infrastructure;

import java.io.Serializable;

public record PedestalTypeDTO(Long id, String manufacturer, String name, String linkVersion, boolean localCardList,
                              Integer localCardListCapacity, int maxSockets) implements Serializable {
}
