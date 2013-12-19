package io.pillopl.dto.card;

import java.io.Serializable;
import java.util.List;

/**
 * The local card list a pedestal controller gets: capacity, entries and how many cards did not fit.
 */
public record LocalCardListDTO(String pedestalCode, int capacity, int houseCards, int cards, int droppedEntries,
                               List<LocalCardListEntryDTO> entries) implements Serializable {
}
