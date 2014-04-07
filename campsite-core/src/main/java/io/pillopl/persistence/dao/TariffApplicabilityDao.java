package io.pillopl.persistence.dao;

import io.pillopl.pricing.TariffApplicability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TariffApplicabilityDao extends JpaRepository<TariffApplicability, Long>, TariffApplicabilityDaoCustom {

    @Query("select a from TariffApplicability a where a.socket.id = :socketId order by a.startDate desc")
    List<TariffApplicability> findBySocketId(@Param("socketId") Long socketId);

    @Query("select a from TariffApplicability a where a.pedestal.id = :pedestalId order by a.startDate desc")
    List<TariffApplicability> findByPedestalId(@Param("pedestalId") Long pedestalId);

    /**
     * Removes the assignments of the socket starting at or after the date (planned ones); older ones stay history.
     */
    @Modifying
    @Query("delete from TariffApplicability a where a.socket.id = :socketId and a.startDate >= :from")
    int deleteBySocketIdStartingFrom(@Param("socketId") Long socketId, @Param("from") LocalDateTime from);

    @Modifying
    @Query("delete from TariffApplicability a where a.pedestal.id = :pedestalId and a.startDate >= :from")
    int deleteByPedestalIdStartingFrom(@Param("pedestalId") Long pedestalId, @Param("from") LocalDateTime from);

    @Query("select count(a) from TariffApplicability a where a.tariff.id = :tariffId")
    long countByTariffId(@Param("tariffId") Long tariffId);
}
