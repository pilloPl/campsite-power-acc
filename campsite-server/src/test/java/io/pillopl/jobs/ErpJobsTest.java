package io.pillopl.jobs;

import io.pillopl.erp.ErpExchangeService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class ErpJobsTest {

    ErpExchangeService erpExchangeService = mock(ErpExchangeService.class);

    @Test
    void accountExportJobOnlyHandsTheWaitingOrdersToTheErp() {
        new ErpAccountExportJob(erpExchangeService).executeInternal(null);

        verify(erpExchangeService).exportPartnerRequests();
        verifyNoMoreInteractions(erpExchangeService);
    }

    @Test
    void accountImportJobOnlyTakesTheErpPartnerAnswers() {
        new ErpAccountImportJob(erpExchangeService).executeInternal(null);

        verify(erpExchangeService).importPartnerAnswers();
        verifyNoMoreInteractions(erpExchangeService);
    }

    @Test
    void billingJobWritesTheLinesBeforeTakingTheBookingAnswers() {
        new ErpBillingExportJob(erpExchangeService).executeInternal(null);

        InOrder order = inOrder(erpExchangeService);
        order.verify(erpExchangeService).exportBillingLines();
        order.verify(erpExchangeService).importBillingAnswers();
    }
}
