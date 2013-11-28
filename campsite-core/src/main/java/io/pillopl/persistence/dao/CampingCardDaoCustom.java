package io.pillopl.persistence.dao;

import io.pillopl.card.CampingCard;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * Hand-written camping card SQL from the SQL map.
 */
public interface CampingCardDaoCustom {

    /**
     * Cards valid on the day whose guests hang in one of the issuer groups, newest first.
     */
    List<CampingCard> findValidCardsForLocalCardList(Collection<Long> issuerGroupIds, LocalDate day);

    /**
     * CARD_UTILS.UPSERT_CARD: updates the living card with the UID (big-endian hex) and number, or inserts it.
     *
     * @return the id of the card
     */
    Long upsertCard(String cardRfidHex, String cardNumber, LocalDate validFrom, LocalDate validTo,
                    String billingAccountNo, Long guestId);
}
