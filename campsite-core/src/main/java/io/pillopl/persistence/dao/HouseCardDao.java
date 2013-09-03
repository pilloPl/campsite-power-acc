package io.pillopl.persistence.dao;

import io.pillopl.card.HouseCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface HouseCardDao extends JpaRepository<HouseCard, Long> {

    /**
     * Active house cards of the groups (e.g. the parent line of a pedestal's group).
     */
    @Query("select h from HouseCard h where h.deleted = false and h.active = true and h.group.id in (:groupIds)")
    List<HouseCard> findActiveByGroupIds(@Param("groupIds") Collection<Long> groupIds);

    /**
     * All house cards of the groups the console shows (deactivated ones too, deleted ones not).
     */
    @Query("select h from HouseCard h where h.deleted = false and h.group.id in (:groupIds) order by h.creationDate, h.id")
    List<HouseCard> findForGroupList(@Param("groupIds") Collection<Long> groupIds);

    @Query("select h from HouseCard h where h.deleted = false and h.rfid = :rfid and (:cardNumber is null or h.cardNumber = :cardNumber)")
    List<HouseCard> findByRfidAndCardNumber(@Param("rfid") String rfid, @Param("cardNumber") String cardNumber);
}
