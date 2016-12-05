package io.pillopl.persistence.dao;

import io.pillopl.monitoring.PedestalCommunicationLog;
import io.pillopl.monitoring.PedestalCommunicationLog.LogType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PedestalCommunicationLogDao extends JpaRepository<PedestalCommunicationLog, Long> {

    /**
     * Logs of one type of a pedestal, newest first.
     */
    @Query("select l from PedestalCommunicationLog l where l.pedestal.id = :pedestalId and l.logType = :logType"
            + " order by l.date desc, l.id desc")
    List<PedestalCommunicationLog> getLogs(@Param("pedestalId") Long pedestalId, @Param("logType") LogType logType);

    @Query("select l from PedestalCommunicationLog l where l.pedestal.id = :pedestalId and l.date >= :from"
            + " and l.date <= :to order by l.date desc, l.id desc")
    List<PedestalCommunicationLog> findByPedestalBetween(@Param("pedestalId") Long pedestalId,
                                                         @Param("from") java.time.LocalDateTime from,
                                                         @Param("to") java.time.LocalDateTime to,
                                                         org.springframework.data.domain.Pageable pageable);
}
