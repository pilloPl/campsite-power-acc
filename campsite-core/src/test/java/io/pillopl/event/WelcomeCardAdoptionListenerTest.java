package io.pillopl.event;

import io.pillopl.group.attribute.WelcomeCardAdoptionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WelcomeCardAdoptionListenerTest {

    private final WelcomeCardAdoptionService welcomeCardAdoptionService = mock(WelcomeCardAdoptionService.class);

    private WelcomeCardAdoptionListener listener;

    @BeforeEach
    void setUp() {
        listener = new WelcomeCardAdoptionListener(new EventService(), new EventTaskExecutorFactory(),
                welcomeCardAdoptionService) {
            @Override
            protected AsyncTaskExecutor createEventTaskExecutor(EventTaskExecutorFactory factory) {
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
    void refusedCardIsCheckedForAdoptionAtItsPedestal() {
        listener.handleEvent(new CheckWelcomeCardAdoptionEvent("HR-MARKO-P001", "8001"));

        verify(welcomeCardAdoptionService).checkAndAdopt("HR-MARKO-P001", "8001");
    }

    @Test
    void failedAdoptionCheckIsOnlyLogged() {
        when(welcomeCardAdoptionService.checkAndAdopt("HR-MARKO-P001", "8001"))
                .thenThrow(new IllegalStateException("no owner"));

        assertThatCode(() -> listener.handleEvent(new CheckWelcomeCardAdoptionEvent("HR-MARKO-P001", "8001")))
                .doesNotThrowAnyException();
    }
}
