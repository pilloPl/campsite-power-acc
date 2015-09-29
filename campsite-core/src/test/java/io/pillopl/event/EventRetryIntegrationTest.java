package io.pillopl.event;

import io.pillopl.infrastructure.SocketStatus;
import io.pillopl.persistence.dao.EventRetryDao;
import io.pillopl.test.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;

/**
 * Failed event sends kept in EVENT_RETRY and sent again, on the database.
 */
class EventRetryIntegrationTest extends IntegrationTest {

    static final String QUEUE = EventService.createEventSystemName("eventRetryIntegrationTest");

    @MockitoSpyBean EventService eventService;
    @Autowired EventRetryService eventRetryService;
    @Autowired EventRetryDao eventRetryDao;
    @Autowired JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        reset(eventService);
        eventService.unregisterQueue(QUEUE, EventType.SOCKET_STATE_NOTIFICATION);
        eventRetryDao.deleteAll();
    }

    @Test
    void refusedEventIsKeptAndDeliveredWithItsAttributesByTheRetry() throws Exception {
        LocalDateTime at = LocalDateTime.of(2026, 7, 14, 10, 30);
        Event event = new Event(EventType.SOCKET_STATE_NOTIFICATION)
                .setAttribute(EventAttribute.PEDESTAL_ID, 42L)
                .setAttribute(EventAttribute.SOCKET_STATUS, SocketStatus.FAULTED)
                .setAttribute(EventAttribute.TIME, at);
        doReturn(false).when(eventService).offer(any());

        assertThat(eventRetryService.sendOrRetryLater(event)).isFalse();

        assertThat(jdbcTemplate.queryForObject("SELECT STATUS FROM EVENT_RETRY WHERE EVENT_ID = ?", String.class,
                event.getId())).isEqualTo("OPEN");

        reset(eventService);
        eventService.registerQueue(QUEUE, EventType.SOCKET_STATE_NOTIFICATION);
        BlockingQueue<Event> queue = eventService.getQueueAndRegisterListener(QUEUE);
        queue.clear();

        assertThat(eventRetryService.retryOpenEvents(10)).isEqualTo(1);

        Event delivered = queue.poll();
        assertThat(delivered).isNotNull();
        assertThat(delivered.getAttribute(EventAttribute.PEDESTAL_ID)).isEqualTo(42L);
        assertThat(delivered.getAttribute(EventAttribute.SOCKET_STATUS)).isEqualTo(SocketStatus.FAULTED);
        assertThat(delivered.getAttribute(EventAttribute.TIME)).isEqualTo(at);
        assertThat(eventRetryDao.findAll()).singleElement().satisfies(retry -> {
            assertThat(retry.getStatus()).isEqualTo(EventRetry.Status.SENT);
            assertThat(retry.getRetryCount()).isEqualTo(1);
        });
        assertThat(eventRetryService.retryOpenEvents(10)).isZero();
    }

    @Test
    void unknownRetryStatusIsRefusedByTheDatabase() {
        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO EVENT_RETRY (EVENT_ID, EVENT_TYPE, ATTRIBUTES,"
                + " STATUS, CREATE_DATE) VALUES ('x', 'BOOT_NOTIFICATION', '', 'LOST', now())"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
