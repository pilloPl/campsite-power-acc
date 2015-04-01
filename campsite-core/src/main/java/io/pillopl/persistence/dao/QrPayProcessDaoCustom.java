package io.pillopl.persistence.dao;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Archive of the finished QR payment processes: the chains are walked with recursive SQL from the SQL map.
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
}
