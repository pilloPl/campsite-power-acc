package io.pillopl.card;

import java.time.LocalDate;

/**
 * One card on the local card list of a pedestal controller: the UID, the last valid day (none for house cards),
 * whether it is a house card and whether it is active (a differential update with an inactive entry removes the
 * card from the controller's list).
 */
public record LocalCardListEntry(String rfid, LocalDate validTo, boolean houseCard, boolean active) {

    public static LocalCardListEntry of(HouseCard houseCard) {
        return new LocalCardListEntry(houseCard.getRfid(), null, true, houseCard.isActive() && !houseCard.isDeleted());
    }

    public static LocalCardListEntry of(CampingCard card) {
        return new LocalCardListEntry(card.getCardRfid(), card.getValidTo(), false, card.isActive());
    }
}
