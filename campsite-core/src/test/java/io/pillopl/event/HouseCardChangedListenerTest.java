package io.pillopl.event;

import io.pillopl.card.LocalCardListService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class HouseCardChangedListenerTest {

    private final LocalCardListService localCardListService = mock(LocalCardListService.class);

    private HouseCardChangedListener listener;

    @BeforeEach
    void setUp() {
        listener = new HouseCardChangedListener(new EventService(), new EventTaskExecutorFactory(),
                localCardListService) {
            @Override
            protected AsyncTaskExecutor newListenerExecutor(EventTaskExecutorFactory factory) {
                return new TaskExecutorAdapter(Runnable::run);
            }
        };
        listener.postcreate();
    }

    @AfterEach
    void tearDown() {
        listener.tryShutdown();
    }

    @Test
    void changedHouseCardUpdatesTheLocalLists() {
        listener.handleEvent(new HouseCardChangedEvent(42L));

        verify(localCardListService).updateLocalListWith(42L);
    }
}
