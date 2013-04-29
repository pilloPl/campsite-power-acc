package io.pillopl.infrastructure;

import java.util.List;

/**
 * A page of the pedestal list with the total count of the filter.
 */
public record PedestalPage(List<Pedestal> pedestals, long total) {
}
