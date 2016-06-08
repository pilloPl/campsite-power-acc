package io.pillopl.event;

import io.pillopl.monitoring.PedestalMailService;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AlertMailListenersTest {

    final PedestalMailService pedestalMailService = mock(PedestalMailService.class);

    PowerSectionMisconfiguredListener powerSectionListener;
    QrPayErrorListener qrPayErrorListener;

    @BeforeEach
    void setUp() {
        powerSectionListener = new PowerSectionMisconfiguredListener(new EventService(),
                new EventTaskExecutorFactory(), pedestalMailService) {
            @Override
            protected AsyncTaskExecutor newListenerExecutor(EventTaskExecutorFactory factory) {
                return new TaskExecutorAdapter(Runnable::run);
            }
        };
        qrPayErrorListener = new QrPayErrorListener(new EventService(), new EventTaskExecutorFactory(),
                pedestalMailService) {
            @Override
            protected AsyncTaskExecutor newListenerExecutor(EventTaskExecutorFactory factory) {
                return new TaskExecutorAdapter(Runnable::run);
            }
        };
        powerSectionListener.postcreate();
        qrPayErrorListener.postcreate();
    }

    @AfterEach
    void tearDown() {
        powerSectionListener.tryShutdown();
        qrPayErrorListener.tryShutdown();
    }

    @Test
    void misconfiguredPowerSectionIsMailedAsAnAlert() throws MessagingException {
        powerSectionListener.handleEvent(new PowerSectionMisconfiguredEvent(1001L, 77L, "HR-BAY-P001", 2,
                "No budget algorithm"));

        verify(pedestalMailService).sendPowerSectionMisconfiguredMail("HR-BAY-P001", 1001L, 2, "No budget algorithm");
    }

    @Test
    void qrPaymentErrorIsMailed() throws MessagingException {
        Event event = new Event(EventType.QRPAY_ERROR);
        event.setAttribute(EventAttribute.ID, 5005L);
        event.setAttribute(EventAttribute.SOCKET_ID, 77L);
        event.setAttribute(EventAttribute.STATUS, "CALCULATION_FAILED");
        event.setAttribute(EventAttribute.ERROR_MESSAGE, "no tariff");

        qrPayErrorListener.handleEvent(event);

        verify(pedestalMailService).sendQrPayErrorMail(77L, 5005L, "CALCULATION_FAILED", "no tariff");
    }

    @Test
    void failingMailDoesNotBreakTheListener() throws MessagingException {
        doThrow(new MessagingException("no SMTP")).when(pedestalMailService)
                .sendQrPayErrorMail(any(), any(), any(), any());
        Event event = new Event(EventType.QRPAY_ERROR);
        event.setAttribute(EventAttribute.ID, 5005L);

        qrPayErrorListener.handleEvent(event);
        qrPayErrorListener.handleEvent(event);

        verify(pedestalMailService, org.mockito.Mockito.times(2)).sendQrPayErrorMail(null, 5005L, null, null);
    }
}
