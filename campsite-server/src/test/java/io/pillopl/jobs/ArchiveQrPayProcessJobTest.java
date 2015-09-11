package io.pillopl.jobs;

import io.pillopl.qrpay.QrPayProcessService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ArchiveQrPayProcessJobTest {

    @Test
    void jobOnlyAsksTheQrPaymentToArchiveFinishedProcesses() {
        QrPayProcessService qrPayProcessService = mock(QrPayProcessService.class);

        new ArchiveQrPayProcessJob(qrPayProcessService).executeInternal(null);

        verify(qrPayProcessService).archiveFinishedProcesses();
    }
}
