package io.pillopl.persistence.dao;

import io.pillopl.card.CampingCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CampingCardDao extends JpaRepository<CampingCard, Long>, CampingCardDaoCustom {

    /**
     * The living card with the UID the pedestal sent (the newest one when the UID was issued twice).
     */
    @Query("select c from CampingCard c where c.deleted = false and c.cardRfid = :cardRfid order by c.id desc limit 1")
    CampingCard findByCardRfid(@Param("cardRfid") String cardRfid);

    @Query("select c from CampingCard c where c.deleted = false and c.cardRfid = :cardRfid order by c.id")
    List<CampingCard> findNotDeletedByCardRfid(@Param("cardRfid") String cardRfid);

    @Query("select c from CampingCard c where c.deleted = false and c.cardNumber = :cardNumber order by c.id")
    List<CampingCard> findNotDeletedByCardNumber(@Param("cardNumber") String cardNumber);

    @Query("select c from CampingCard c where c.deleted = false and c.guest.id = :guestId order by c.id")
    List<CampingCard> findByGuestId(@Param("guestId") Long guestId);
}
