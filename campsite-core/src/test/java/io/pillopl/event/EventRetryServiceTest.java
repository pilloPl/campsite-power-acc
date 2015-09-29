package io.pillopl.event;

import io.pillopl.persistence.dao.EventRetryDao;
import io.pillopl.util.DateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventRetryServiceTest {

    final EventService eventService = mock(EventService.class);
    final EventRetryDao eventRetryDao = mock(EventRetryDao.class);
    final DateService dateService = mock(DateService.class);
    final EventRetryService service = new EventRetryService(eventService, eventRetryDao, dateService);
    final LocalDateTime now = LocalDateTime.of(2026, 7, 14, 10, 30);
    final Event event = new Event(EventType.SOCKET_STATE_NOTIFICATION).setAttribute(EventAttribute.PEDESTAL_ID, 5L);

    @BeforeEach
    void setUp() {
        when(dateService.getNow()).thenReturn(now);
        when(eventRetryDao.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void eventTakenByTheQueuesIsNotKept() {
        when(eventService.offer(event)).thenReturn(true);

        assertThat(service.sendOrRetryLater(event)).isTrue();

        verify(eventRetryDao, never()).save(any());
    }

    @Test
    void eventRefusedByAQueueIsKeptForARetry() {
        when(eventService.offer(event)).thenReturn(false);

        assertThat(service.sendOrRetryLater(event)).isFalse();

        EventRetry kept = savedRetry();
        assertThat(kept.getEventType()).isEqualTo(EventType.SOCKET_STATE_NOTIFICATION);
        assertThat(kept.getFailure()).isEqualTo(EventRetryService.QUEUE_REFUSED);
        assertThat(kept.getCreateDate()).isEqualTo(now);
    }

    @Test
    void failingSendIsKeptWithItsFailure() {
        when(eventService.offer(event)).thenThrow(new IllegalStateException("listener gone"));

        assertThat(service.sendOrRetryLater(event)).isFalse();

        assertThat(savedRetry().getFailure()).isEqualTo("listener gone");
    }

    @Test
    void retryThatCanNotBeKeptOnlyLogs() {
        when(eventService.offer(event)).thenReturn(false);
        when(eventRetryDao.save(any())).thenThrow(new IllegalStateException("database down"));

        assertThat(service.sendOrRetryLater(event)).isFalse();
    }

    @Test
    void openEventsAreSentAgainAndTheFailingOnesCountTheirAttempts() {
        EventRetry goesThrough = EventRetry.of(event, "first", now.minusMinutes(5));
        EventRetry failsAgain = EventRetry.of(new Event(EventType.BOOT_NOTIFICATION), "first", now.minusMinutes(5));
        when(eventRetryDao.findByStatusOrderByIdAsc(EventRetry.Status.OPEN)).thenReturn(List.of(goesThrough,
                failsAgain));
        when(eventService.offer(any())).thenAnswer(invocation ->
                invocation.<Event>getArgument(0).getType() == EventType.SOCKET_STATE_NOTIFICATION);

        assertThat(service.retryOpenEvents(10)).isEqualTo(1);

        assertThat(goesThrough.getStatus()).isEqualTo(EventRetry.Status.SENT);
        assertThat(failsAgain.getStatus()).isEqualTo(EventRetry.Status.OPEN);
        assertThat(failsAgain.getRetryCount()).isEqualTo(1);
        assertThat(failsAgain.getLastRetryDate()).isEqualTo(now);
    }

    @Test
    void eventOfTheLastAttemptIsGivenUp() {
        EventRetry retry = EventRetry.of(event, "first", now.minusMinutes(5));
        when(eventRetryDao.findByStatusOrderByIdAsc(EventRetry.Status.OPEN)).thenReturn(List.of(retry));
        when(eventService.offer(any())).thenReturn(false);

        service.retryOpenEvents(1);

        assertThat(retry.getStatus()).isEqualTo(EventRetry.Status.FAILED);
    }

    @Test
    void attributesThatCanNotBeReadBackFailTheAttempt() {
        EventRetry broken = EventRetry.of(event, "first", now);
        broken.setAttributes("PEDESTAL_ID=not-a-number\n");
        when(eventRetryDao.findByStatusOrderByIdAsc(EventRetry.Status.OPEN)).thenReturn(List.of(broken));

        assertThat(service.retryOpenEvents(3)).isZero();

        assertThat(broken.getRetryCount()).isEqualTo(1);
        verify(eventService, never()).offer(any());
    }

    private EventRetry savedRetry() {
        ArgumentCaptor<EventRetry> captor = ArgumentCaptor.forClass(EventRetry.class);
        verify(eventRetryDao).save(captor.capture());
        return captor.getValue();
    }
}
