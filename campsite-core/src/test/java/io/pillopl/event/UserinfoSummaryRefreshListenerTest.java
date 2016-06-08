package io.pillopl.event;

import io.pillopl.user.UserinfoChangeType;
import io.pillopl.user.UserinfoSummaryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class UserinfoSummaryRefreshListenerTest {

    private final List<Runnable> pendingTasks = new ArrayList<>();

    private final UserinfoSummaryService userinfoSummaryService = mock(UserinfoSummaryService.class);

    private UserinfoSummaryRefreshListener listener;

    @BeforeEach
    void setUp() {
        TaskExecutor collecting = pendingTasks::add;
        listener = new UserinfoSummaryRefreshListener(new EventService(), new EventTaskExecutorFactory(),
                userinfoSummaryService) {
            @Override
            protected AsyncTaskExecutor newListenerExecutor(EventTaskExecutorFactory factory) {
                return new TaskExecutorAdapter(collecting);
            }
        };
        listener.postcreate();
    }

    @AfterEach
    void tearDown() {
        listener.tryShutdown();
    }

    @Test
    void changesArrivingWhileARefreshIsPendingShareIt() {
        listener.handleEvent(changed("a"));
        listener.handleEvent(changed("b"));
        listener.handleEvent(changed("c"));

        assertThat(pendingTasks).hasSize(1);
        pendingTasks.get(0).run();
        verify(userinfoSummaryService, times(1)).refresh();
    }

    @Test
    void aChangeAfterTheRefreshStartedAsksForANewRefresh() {
        listener.handleEvent(changed("a"));
        pendingTasks.get(0).run();

        listener.handleEvent(changed("b"));

        assertThat(pendingTasks).hasSize(2);
    }

    private static Event changed(String username) {
        return new UserinfoChangedEvent(1L, username, UserinfoChangeType.NAME_CHANGED);
    }
}
