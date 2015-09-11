package io.pillopl.persistence.dao;

import io.pillopl.qrpay.QrPayProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Rows of the QR payment processes. The current row of a process is the one without successor; the archive of the
 * finished chains is hand-written SQL in {@link QrPayProcessDaoCustom}.
 */
public interface QrPayProcessDao extends JpaRepository<QrPayProcess, Long>, QrPayProcessDaoCustom {

    @Query(value = "select nextval('QRPAY_PROCESS_ID_SEQ')", nativeQuery = true)
    Long nextRowId();

    @Query("select p from QrPayProcess p where p.processId = :processId and p.nextProcessId is null")
    QrPayProcess findCurrentByProcessId(@Param("processId") Long processId);

    @Query("select p from QrPayProcess p where p.orderReference = :orderReference and p.nextProcessId is null")
    QrPayProcess findCurrentByOrderReference(@Param("orderReference") String orderReference);

    @Query("select p from QrPayProcess p where p.socketId = :socketId and p.nextProcessId is null"
            + " and p.state not in ('CLOSED', 'ERROR') order by p.id")
    List<QrPayProcess> findOpenBySocketId(@Param("socketId") Long socketId);

    /**
     * The whole chain of a process, first row first.
     */
    List<QrPayProcess> findByProcessIdOrderById(Long processId);
}
