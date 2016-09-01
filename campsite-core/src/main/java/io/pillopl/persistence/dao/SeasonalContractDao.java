package io.pillopl.persistence.dao;

import io.pillopl.guest.SeasonalContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SeasonalContractDao extends JpaRepository<SeasonalContract, Long> {

    /**
     * Contracts of the guest's billing accounts not ended and started on or before the day, latest start first (the
     * day against end and requested end is checked by {@link SeasonalContract#isActiveOn}).
     */
    @Query("select c from SeasonalContract c where c.billingAccount.guest.id = :guestId"
            + " and c.status <> io.pillopl.guest.ContractStatus.ENDED and c.startDate <= :day"
            + " order by c.startDate desc, c.id desc")
    List<SeasonalContract> findStartedContractsOfGuest(@Param("guestId") Long guestId, @Param("day") LocalDate day);

    /**
     * Contracts of a billing account touching the period, for the seasonal billing.
     */
    @Query("select c from SeasonalContract c where c.billingAccount.id = :billingAccountId and c.startDate <= :to"
            + " and (c.endDate is null or c.endDate >= :from) order by c.startDate, c.id")
    List<SeasonalContract> findContractsOfAccountIn(@Param("billingAccountId") Long billingAccountId,
                                                     @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select c from SeasonalContract c where c.siteGroup.id = :siteGroupId and c.pitchNo = :pitchNo"
            + " and c.status <> io.pillopl.guest.ContractStatus.ENDED order by c.startDate desc")
    List<SeasonalContract> findRunningContractsOnPitch(@Param("siteGroupId") Long siteGroupId,
                                                       @Param("pitchNo") String pitchNo);

    /**
     * Contracts on pitches of the campsites or of guests of the issuer groups (a contract without pitch belongs to
     * its guest's issuer), optionally of one status; latest start first.
     */
    @Query("select c from SeasonalContract c where (c.siteGroup.id in (:siteGroupIds)"
            + " or c.billingAccount.guest.group.id in (:issuerGroupIds))"
            + " and (:status is null or c.status = :status) order by c.startDate desc, c.id desc")
    List<SeasonalContract> findContractsInScope(@Param("siteGroupIds") java.util.Collection<Long> siteGroupIds,
                                                @Param("issuerGroupIds") java.util.Collection<Long> issuerGroupIds,
                                                @Param("status") io.pillopl.guest.ContractStatus status);

    @Query("select c from SeasonalContract c where c.billingAccount.guest.id = :guestId order by c.startDate desc")
    List<SeasonalContract> findContractsOfGuest(@Param("guestId") Long guestId);
}
