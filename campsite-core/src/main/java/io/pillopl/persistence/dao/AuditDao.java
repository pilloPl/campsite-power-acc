package io.pillopl.persistence.dao;

import io.pillopl.audit.Audit;
import io.pillopl.audit.AuditCategory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditDao extends JpaRepository<Audit, Long>, AuditDaoCustom {

    /**
     * Audit search of the console: every filter is optional (null), all values are bound.
     */
    @Query("select a from Audit a where "
            + "(:nameFilter is null or lower(a.actionName) like lower(cast(:nameFilter as string))) "
            + "and (:typeFilter is null or lower(a.actionType) like lower(cast(:typeFilter as string))) "
            + "and (:statusFilter is null or lower(a.status) like lower(cast(:statusFilter as string))) "
            + "and (:categoryFilter is null or a.category = :categoryFilter) "
            + "and (cast(:dateFrom as localdatetime) is null or (a.actionDate is not null and a.actionDate >= :dateFrom) "
            + "     or (a.actionDate is null and a.dateCreated >= :dateFrom)) "
            + "and (cast(:dateTo as localdatetime) is null or (a.actionDate is not null and a.actionDate <= :dateTo) "
            + "     or (a.actionDate is null and a.dateCreated <= :dateTo)) "
            + "and (:freeSearch is null or "
            + "   (lower(a.userId) like concat('%', lower(cast(:freeSearch as string)), '%') "
            + "   or lower(a.actionGroupId) like concat('%', lower(cast(:freeSearch as string)), '%') "
            + "   or lower(a.errorReason) like concat('%', lower(cast(:freeSearch as string)), '%') "
            + "   or lower(a.managedServer) like concat('%', lower(cast(:freeSearch as string)), '%') "
            + "   or lower(a.infoField1) like concat('%', lower(cast(:freeSearch as string)), '%') "
            + "   or lower(a.infoField2) like concat('%', lower(cast(:freeSearch as string)), '%') "
            + "   or lower(a.message) like concat('%', lower(cast(:freeSearch as string)), '%') "
            + "   or lower(a.auditData) like concat('%', lower(cast(:freeSearch as string)), '%')))")
    List<Audit> findAuditEntries(@Param("nameFilter") String nameFilter,
                                 @Param("typeFilter") String typeFilter,
                                 @Param("statusFilter") String statusFilter,
                                 @Param("categoryFilter") AuditCategory categoryFilter,
                                 @Param("dateFrom") LocalDateTime dateFrom,
                                 @Param("dateTo") LocalDateTime dateTo,
                                 @Param("freeSearch") String freeSearch,
                                 Pageable pageable);

    @Query("select a from Audit a where a.actionDate >= :from and a.actionDate <= :to "
            + "and lower(a.actionType) = lower('SOCKET_OUT_OF_SERVICE') order by a.actionDate asc")
    List<Audit> findOutOfServiceEntries(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
