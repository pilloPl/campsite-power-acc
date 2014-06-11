package io.pillopl.dto.pedestallink.v1;

import io.pillopl.dto.card.LocalCardListEntryDTO;

import java.io.Serializable;
import java.util.List;

/**
 * Link v1, pedestal backend to pedestal: the local card list of the controller. Update type FULL replaces the list,
 * DIFFERENTIAL adds or changes the entries and removes the removed cards, CLEAR empties it.
 */
public record SendLocalListRequest(int listVersion, String updateType, List<LocalCardListEntryDTO> entries,
                                   List<String> removedRfids) implements Serializable {

    public static final String FULL = "FULL";

    public static final String DIFFERENTIAL = "DIFFERENTIAL";

    public static final String CLEAR = "CLEAR";
}
