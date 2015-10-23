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

    /**
     * Nightly sync state of the cards towards the issuer partners: cards whose stay or season covers {@code today}
     * want an ADD, cards whose stay or season is over want a DELETE; the matrix of
     * {@link io.pillopl.issuer.PartnerSyncState} turns the wish into what is exported.
     *
     * @return the number of cards looked at
     */
    int updateExpiredCardsPartnerSyncState(LocalDate today);

    /**
     * Cards of the guests in the subtree of the ISSUER group whose sync state asks for an export, oldest first.
     */
    List<CampingCard> findCardsForPartnerExport(Long issuerGroupId);
}
