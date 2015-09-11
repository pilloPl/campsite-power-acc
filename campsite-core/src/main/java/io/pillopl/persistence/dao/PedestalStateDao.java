package io.pillopl.persistence.dao;

import io.pillopl.infrastructure.PedestalState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PedestalStateDao extends JpaRepository<PedestalState, Long> {

    @Query("select s from PedestalState s where s.pedestal.id = :pedestalId")
    PedestalState findByPedestalId(@Param("pedestalId") Long pedestalId);

    @Query("select s from PedestalState s where s.timeout = false and s.approxLastIncoming < :before")
    List<PedestalState> findAliveWithLastIncomingBefore(@Param("before") LocalDateTime before);

    @Query("select s from PedestalState s where s.approxLastIncoming < :before and s.pedestal.deleted = false"
            + " order by s.approxLastIncoming")
    List<PedestalState> findWithLastIncomingBefore(@Param("before") LocalDateTime before);
}
