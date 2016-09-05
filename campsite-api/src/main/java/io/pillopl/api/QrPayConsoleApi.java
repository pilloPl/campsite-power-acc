package io.pillopl.api;

import io.pillopl.commons.DateRange;
import io.pillopl.commons.ListPage;
import io.pillopl.commons.Pagination;
import io.pillopl.dto.qrpay.QrPayProcessOverviewDTO;
import io.pillopl.dto.qrpay.QrPayTimelineDTO;
import io.pillopl.value.Username;

/**
 * QR payments in the console (the payment app uses {@link QrPayApi}): list and timeline need REPORT_VIEW, cancelling
 * a stuck process and the archive run need BILLING_EDIT. Only processes at the pedestals of the caller's SITE scope.
 */
public interface QrPayConsoleApi {

    /**
     * @param state           null for all states
     * @param campsiteGroupId null for the whole SITE scope of the caller
     * @param period          of the request date, open ends allowed
     */
    ListPage<QrPayProcessOverviewDTO> findQrPayProcesses(Username authUsername, String state, Long campsiteGroupId,
                                                         DateRange period, Pagination pagination);

    QrPayTimelineDTO getQrPayTimeline(Username authUsername, Long processId);

    /**
     * Drives a stuck process through its states to an end: a running supply is stopped (and priced when the
     * pedestal reports the stop), a process without supply goes to ERROR.
     *
     * @throws io.pillopl.api.exception.ConflictException for a finished process or one not stuck yet
     */
    QrPayTimelineDTO cancelQrPayProcess(Username authUsername, Long processId);

    /**
     * Moves the finished chains older than QrPayArchiveDelayInHours to the archive now.
     *
     * @return the number of archived rows
     */
    int runQrPayArchive(Username authUsername);
}
