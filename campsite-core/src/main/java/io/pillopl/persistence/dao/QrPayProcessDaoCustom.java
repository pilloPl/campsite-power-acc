package io.pillopl.persistence.dao;

import io.pillopl.commons.ListPage;
import io.pillopl.commons.Pagination;
import io.pillopl.dto.qrpay.QrPayProcessOverviewDTO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Archive of the finished QR payment processes (the chains are walked with recursive SQL from the SQL map) and the
 * console view of the live and archived processes.
 */
public interface QrPayProcessDaoCustom {

    /**
     * @return ids of all rows of the chains whose current row is CLOSED or ERROR since before the given time
     */
    List<Long> findFinishedProcessRowIds(LocalDateTime finishedBefore);

    /**
     * Copies the rows into QRPAY_PROCESS_ARCHIVE with their ids and links.
     *
     * @return number of archived rows
     */
    int archiveRows(List<Long> rowIds, LocalDateTime archiveDate);

    /**
     * Deletes the rows; whole chains only (the links between the rows are checked at the end of the statement).
     *
     * @return number of deleted rows
     */
    int deleteRows(List<Long> rowIds);

    /**
     * Current rows of the processes at the pedestals of the SITE subtree, live and archived, newest request first.
     *
     * @param campsiteGroupId null for the whole subtree
     * @param state           null for all states
     * @param stuckBefore     an unfinished process whose state began before is marked stuck
     */
    ListPage<QrPayProcessOverviewDTO> findProcessOverviewPage(Long userSiteGroupId, Long campsiteGroupId, String state,
                                                              LocalDateTime from, LocalDateTime to,
                                                              LocalDateTime stuckBefore, Pagination pagination);

    /**
     * All rows of a process, live or archived, first row first; empty for an unknown process.
     */
    List<QrPayChainRow> findChain(Long processId);

    /**
     * A row of a process chain as the timeline shows it.
     */
    record QrPayChainRow(Long id, Long processId, String orderReference, Long socketId, String state,
                         LocalDateTime startTime, LocalDateTime stopTime, Integer transactionId, Long supplySessionId,
                         String errorCode, String errorMessage, boolean archived) {
    }
}
