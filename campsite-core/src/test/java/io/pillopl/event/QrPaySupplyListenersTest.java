package io.pillopl.event;

import io.pillopl.qrpay.QrPayProcessSupervisor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class QrPaySupplyListenersTest {

    final QrPayProcessSupervisor supervisor = mock(QrPayProcessSupervisor.class);

    QrPaySupplyStartedListener startedListener;
    QrPaySupplyStoppedListener stoppedListener;

    @BeforeEach
    void setUp() {
        startedListener = new QrPaySupplyStartedListener(new EventService(), new EventTaskExecutorFactory(),
                supervisor) {
            @Override
            protected AsyncTaskExecutor createEventTaskExecutor(EventTaskExecutorFactory factory) {
                return new TaskExecutorAdapter(Runnable::run);
            }
        };
        stoppedListener = new QrPaySupplyStoppedListener(new EventService(), new EventTaskExecutorFactory(),
                supervisor) {
            @Override
            protected AsyncTaskExecutor createEventTaskExecutor(EventTaskExecutorFactory factory) {
                return new TaskExecutorAdapter(Runnable::run);
            }
        };
        startedListener.postcreate();
        stoppedListener.postcreate();
    }

    @AfterEach
    void tearDown() {
        startedListener.tryShutdown();
        stoppedListener.tryShutdown();
    }

    @Test
    void supplyStartedWithAQrAuthTagSignalsThePayment() {
        startedListener.handleEvent(supplyEvent(EventType.SUPPLY_STARTED, "QR1005"));

        verify(supervisor).onSupplyStarted(1005L, 77L, 1201);
    }

    @Test
    void supplyStoppedWithAQrAuthTagSignalsThePayment() {
        stoppedListener.handleEvent(supplyEvent(EventType.SUPPLY_STOPPED, "QR1005"));

        verify(supervisor).onSupplyStopped(1005L, 77L, 1201);
    }

    @Test
    void suppliesOfCardsAreNotRelevantForTheQrPayment() {
        startedListener.handleEvent(supplyEvent(EventType.SUPPLY_STARTED, "04A2B3C4"));
        stoppedListener.handleEvent(supplyEvent(EventType.SUPPLY_STOPPED, null));

        verifyNoInteractions(supervisor);
    }

    @Test
    void failingPaymentDoesNotBreakTheListener() {
        org.mockito.Mockito.doThrow(new IllegalStateException("boom")).when(supervisor)
                .onSupplyStarted(any(), any(), any());

        startedListener.handleEvent(supplyEvent(EventType.SUPPLY_STARTED, "QR1005"));

        verify(supervisor).onSupplyStarted(1005L, 77L, 1201);
    }

    private static Event supplyEvent(EventType type, String rfid) {
        Event event = new Event(type);
        event.setAttribute(EventAttribute.RFID, rfid);
        event.setAttribute(EventAttribute.SUPPLY_SESSION_ID, 77L);
        event.setAttribute(EventAttribute.TRANSACTION_ID, 1201);
        return event;
    }
}
