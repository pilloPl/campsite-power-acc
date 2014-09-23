package io.pillopl.persistence.dao;

import io.pillopl.monitoring.SocketStatusLog;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * Monitoring log of the sockets, the error masks of the controllers, the status report and the utilization view.
 */
public interface SocketStatusLogDao extends JpaRepository<SocketStatusLog, Long> {

    /**
     * One row of the status report: seconds a socket spent in a status during the month.
     */
    interface StatusReportRow {

        Long getSocketId();

        String getSocketName();

        Long getPedestalId();

        Long getGroupId();

        Long getTimeInSeconds();

        String getStatus();
    }

    @Query("select l from SocketStatusLog l where l.socket.id = :socketId order by l.createDate desc, l.id desc")
    List<SocketStatusLog> findLastsBySocketId(@Param("socketId") Long socketId, Pageable pageable);

    default SocketStatusLog findLastBySocketId(Long socketId) {
        List<SocketStatusLog> logs = findLastsBySocketId(socketId, PageRequest.of(0, 1));
        return logs.isEmpty() ? null : logs.get(0);
    }

    /**
     * Mask of an error code: the row of the manufacturer, otherwise the row of the link protocol ('DEFAULT').
     */
    @Query(value = "SELECT e.MASK FROM PEDESTAL_ERROR_CODE e"
            + " WHERE e.CODE = :code AND e.MANUFACTURER IN (:manufacturer, 'DEFAULT')"
            + " ORDER BY CASE WHEN e.MANUFACTURER = :manufacturer THEN 0 ELSE 1 END"
            + " LIMIT 1", nativeQuery = true)
    Long findErrorMask(@Param("manufacturer") String manufacturer, @Param("code") String code);

    /**
     * Abbreviations of the error flags, read with the masks of the manufacturer when it has its own, otherwise
     * with the masks of the link protocol.
     */
    @Query(value = "SELECT e.ABBREVIATION FROM PEDESTAL_ERROR_CODE e"
            + " WHERE e.MANUFACTURER = (CASE WHEN EXISTS (SELECT 1 FROM PEDESTAL_ERROR_CODE m"
            + "                                            WHERE m.MANUFACTURER = :manufacturer)"
            + "                             THEN :manufacturer ELSE 'DEFAULT' END)"
            + " AND ((e.MASKED AND e.MASK <> 0 AND (CAST(:errorFlags AS BIGINT) & e.MASK) = e.MASK)"
            + "      OR (NOT e.MASKED AND e.MASK = :errorFlags))"
            + " ORDER BY e.MASK", nativeQuery = true)
    List<String> findErrorAbbreviations(@Param("manufacturer") String manufacturer,
                                        @Param("errorFlags") long errorFlags);

    @Query(value = "SELECT r.SOCKET_ID AS socketId, r.SOCKET_NAME AS socketName, r.PEDESTAL_ID AS pedestalId,"
            + " r.GROUP_ID AS groupId, r.TIME_IN_SECONDS AS timeInSeconds, r.STATUS AS status"
            + " FROM socket_status_report(CAST(:month AS DATE)) r"
            + " WHERE r.GROUP_ID IN (:groupIds)"
            + " ORDER BY r.SOCKET_ID, r.STATUS", nativeQuery = true)
    List<StatusReportRow> findStatusReport(@Param("month") LocalDate month,
                                           @Param("groupIds") Collection<Long> groupIds);

    @Modifying
    @Query(value = "REFRESH MATERIALIZED VIEW CONCURRENTLY SOCKET_UTILIZATION", nativeQuery = true)
    void refreshSocketUtilization();
}
