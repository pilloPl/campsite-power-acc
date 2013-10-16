package io.pillopl.card;

import java.util.List;

/**
 * The local card list built for a pedestal controller: house cards first, then camping cards, cut to the capacity of
 * the controller. {@code droppedEntries} counts the cards that did not fit.
 */
public record LocalCardList(String pedestalCode, int capacity, List<LocalCardListEntry> entries, int houseCards,
                            int cards, int droppedEntries) {

    public static LocalCardList empty(String pedestalCode, int capacity) {
        return new LocalCardList(pedestalCode, capacity, List.of(), 0, 0, 0);
    }

    public int size() {
        return entries.size();
    }
}
