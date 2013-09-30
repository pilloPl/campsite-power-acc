package io.pillopl.card;

import java.time.LocalDate;

/**
 * One card on the local card list of a pedestal controller: the UID, the last valid day (none for house cards) and
 * whether it is a house card.
 */
public record LocalCardListEntry(String rfid, LocalDate validTo, boolean houseCard) {

    public static LocalCardListEntry of(HouseCard houseCard) {
        return new LocalCardListEntry(houseCard.getRfid(), null, true);
    }

    public static LocalCardListEntry of(CampingCard card) {
        return new LocalCardListEntry(card.getCardRfid(), card.getValidTo(), false);
    }
}
