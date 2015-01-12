package io.pillopl.persistence.dao;

import io.pillopl.guest.Stay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface StayDao extends JpaRepository<Stay, Long> {

    /**
     * Stays the card was handed out for that are not checked out and include the day, latest arrival first.
     */
    @Query("select s from Stay s where s.deleted = false and s.card.id = :cardId and s.checkOutDate is null"
            + " and s.arrivalDate <= :day and s.departureDate >= :day order by s.arrivalDate desc, s.id desc")
    List<Stay> findOpenStaysOfCardOn(@Param("cardId") Long cardId, @Param("day") LocalDate day);

    @Query("select s.id from Stay s where s.id in (:stayIds) and s.checkOutDate < :checkedOutBefore")
    List<Long> findIdsCheckedOutBefore(@Param("stayIds") Collection<Long> stayIds,
                                       @Param("checkedOutBefore") LocalDateTime checkedOutBefore);

    @Query("select s from Stay s where s.deleted = false and s.guest.id = :guestId order by s.arrivalDate desc")
    List<Stay> findByGuestId(@Param("guestId") Long guestId);

    /**
     * Open stays on the pitch of the campsite overlapping the days (a pitch is let to one stay at a time; the
     * departure day of one stay may be the arrival day of the next).
     */
    @Query("select s from Stay s where s.deleted = false and s.siteGroup.id = :siteGroupId and s.pitchNo = :pitchNo"
            + " and s.checkOutDate is null and s.arrivalDate < :departure and s.departureDate > :arrival")
    List<Stay> findOpenStaysOnPitch(@Param("siteGroupId") Long siteGroupId, @Param("pitchNo") String pitchNo,
                                    @Param("arrival") LocalDate arrival, @Param("departure") LocalDate departure);
}
