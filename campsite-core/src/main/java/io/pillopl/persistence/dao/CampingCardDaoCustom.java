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
}
