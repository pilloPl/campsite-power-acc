package io.pillopl.qrpay;

import io.pillopl.card.QrPaySupplyAuthorizer;
import io.pillopl.config.SystemPropertyService;
import io.pillopl.infrastructure.Socket;
import io.pillopl.persistence.dao.QrPayProcessDao;
import io.pillopl.qrpay.state.QrPayState;
import io.pillopl.qrpay.state.QrPayStateFactory;
import io.pillopl.util.DateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The row chain of the QR payment processes: every transition inserts the next row and closes the old one; the
 * finished chains go to the archive. Also tells the card validation whether a QR id tag may supply at a socket.
 */
@Service
public class QrPayProcessService implements QrPaySupplyAuthorizer {

    private static final Logger LOG = LoggerFactory.getLogger(QrPayProcessService.class);

    static final int ARCHIVE_BATCH_SIZE = 500;

    private final QrPayProcessDao qrPayProcessDao;

    private final QrPayStateFactory stateFactory;

    private final DateService dateService;

    private final SystemPropertyService systemPropertyService;

    public QrPayProcessService(QrPayProcessDao qrPayProcessDao, QrPayStateFactory stateFactory,
                               DateService dateService, SystemPropertyService systemPropertyService) {
        this.qrPayProcessDao = qrPayProcessDao;
        this.stateFactory = stateFactory;
        this.dateService = dateService;
        this.systemPropertyService = systemPropertyService;
    }

    /**
     * Writes the process (the state's copy of the current row) as the next row in the given state and links the
     * old row to it. Committed at once when called outside a transaction.
     *
     * @return the state of the new row
     * @throws IllegalStateException when the row is not the current one any more (another signal was faster)
     */
    @Transactional
    public QrPayState transAndSave(QrPayStateName stateName, QrPayProcess process) {
        LOG.debug(String.format("QR payment %s moves to %s", process, stateName));
        QrPayProcess oldRow = qrPayProcessDao.findById(process.getId())
                .orElseThrow(() -> new IllegalStateException("QR payment row " + process.getId() + " not found"));
        if (!oldRow.isCurrent()) {
            throw new IllegalStateException("QR payment row " + oldRow.getId() + " was already followed by row "
                    + oldRow.getNextProcessId());
        }
        LocalDateTime now = dateService.getNow();
        QrPayProcess newRow = process.copyForNextState(qrPayProcessDao.nextRowId(), stateName, now);
        qrPayProcessDao.saveAndFlush(newRow);
        oldRow.setNextProcessId(newRow.getId());
        oldRow.setStopTime(now);
        qrPayProcessDao.saveAndFlush(oldRow);
        LOG.debug(String.format("QR payment state %s replaced by %s", oldRow, newRow));
        return stateFactory.getState(newRow.detachedCopy());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSupplyAuthorized(Long qrPayProcessId, Socket socket, boolean supplyRunning) {
        QrPayProcess current = qrPayProcessDao.findCurrentByProcessId(qrPayProcessId);
        if (current == null) {
            LOG.info(String.format("QR payment %s unknown", qrPayProcessId));
            return false;
        }
        if (socket == null || !current.getSocketId().equals(socket.getId())) {
            LOG.warn(String.format("QR payment %s belongs to socket %s, not to %s", qrPayProcessId,
                    current.getSocketId(), socket));
            return false;
        }
        if (supplyRunning) {
            return true;
        }
        return current.getStateName().isSupplyMayStart();
    }

    @Transactional(readOnly = true)
    public QrPayProcess findCurrent(Long processId) {
        return qrPayProcessDao.findCurrentByProcessId(processId);
    }

    @Transactional(readOnly = true)
    public QrPayProcess findCurrentByOrderReference(String orderReference) {
        return qrPayProcessDao.findCurrentByOrderReference(orderReference);
    }

    /**
     * Other processes still open at the socket (a second guest must not start it).
     */
    @Transactional(readOnly = true)
    public boolean hasOtherOpenProcess(Long socketId, Long processId) {
        return qrPayProcessDao.findOpenBySocketId(socketId).stream()
                .anyMatch(p -> !p.getProcessId().equals(processId));
    }

    /**
     * Moves the chains finished (CLOSED or ERROR) since longer than the archive delay to the archive.
     *
     * @return number of archived rows
     */
    @Transactional
    public int archiveFinishedProcesses() {
        LocalDateTime now = dateService.getNow();
        LocalDateTime finishedBefore = now.minusHours(systemPropertyService.getQrPayArchiveDelayInHours());
        List<Long> rowIds = qrPayProcessDao.findFinishedProcessRowIds(finishedBefore);
        LOG.info(String.format("Found %d QR payment rows to archive", rowIds.size()));
        int archived = 0;
        for (List<Long> batch : batches(rowIds)) {
            archived += qrPayProcessDao.archiveRows(batch, now);
        }
        // whole chains in one statement: the links between the rows are checked at its end
        int deleted = rowIds.isEmpty() ? 0 : qrPayProcessDao.deleteRows(rowIds);
        if (deleted != archived) {
            throw new IllegalStateException(String.format("Archived %d QR payment rows but deleted %d", archived,
                    deleted));
        }
        LOG.info(String.format("Archived %d QR payment rows", archived));
        return archived;
    }

    static List<List<Long>> batches(List<Long> ids) {
        List<List<Long>> batches = new ArrayList<>();
        for (int i = 0; i < ids.size(); i += ARCHIVE_BATCH_SIZE) {
            batches.add(ids.subList(i, Math.min(ids.size(), i + ARCHIVE_BATCH_SIZE)));
        }
        return batches;
    }
}
