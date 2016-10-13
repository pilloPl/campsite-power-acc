package io.pillopl.event;

import io.pillopl.infrastructure.SocketStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EventRetryTest {

    final LocalDateTime now = LocalDateTime.of(2026, 7, 14, 10, 30);

    @Test
    void keptEventIsSentAgainWithTheSameTypeAndTypedAttributes() {
        Event event = new Event(EventType.SOCKET_STATE_NOTIFICATION)
                .setAttribute(EventAttribute.PEDESTAL_ID, 17L)
                .setAttribute(EventAttribute.SOCKET_NO, 2)
                .setAttribute(EventAttribute.SOCKET_STATUS, SocketStatus.BREAKER_TRIPPED)
                .setAttribute(EventAttribute.TIME, now)
                .setAttribute(EventAttribute.METER_VALUE, new BigDecimal("1250.500"))
                .setAttribute(EventAttribute.BILLABLE, Boolean.TRUE)
                .setAttribute(EventAttribute.PEDESTAL_INFO, "door open = yes\nsecond line")
                .setAttribute(EventAttribute.PEDESTAL_ERROR_CODE, null);

        EventRetry retry = EventRetry.of(event, "queue full", now);
        Event again = retry.toEvent();

        assertThat(retry.getEventId()).isEqualTo(event.getId());
        assertThat(retry.getStatus()).isEqualTo(EventRetry.Status.OPEN);
        assertThat(again.getType()).isEqualTo(EventType.SOCKET_STATE_NOTIFICATION);
        assertThat(again.getId()).isNotEqualTo(event.getId());
        assertThat(again.getAttribute(EventAttribute.PEDESTAL_ID)).isEqualTo(17L);
        assertThat(again.getAttribute(EventAttribute.SOCKET_NO)).isEqualTo(2);
        assertThat(again.getAttribute(EventAttribute.SOCKET_STATUS)).isEqualTo(SocketStatus.BREAKER_TRIPPED);
        assertThat(again.getAttribute(EventAttribute.TIME)).isEqualTo(now);
        assertThat(again.getAttribute(EventAttribute.METER_VALUE)).isEqualTo(new BigDecimal("1250.500"));
        assertThat(again.getAttribute(EventAttribute.BILLABLE)).isEqualTo(Boolean.TRUE);
        assertThat(again.getAttribute(EventAttribute.PEDESTAL_INFO)).isEqualTo("door open = yes\nsecond line");
        assertThat(again.getAttributes()).doesNotContainKey(EventAttribute.PEDESTAL_ERROR_CODE);
    }

    @Test
    void attributesAreStoredAsStableText() {
        Event event = new Event(EventType.BOOT_NOTIFICATION)
                .setAttribute(EventAttribute.PEDESTAL_CODE, "HR-BAY-P201")
                .setAttribute(EventAttribute.PEDESTAL_ID, 3L);

        assertThat(EventRetry.of(event, null, now).getAttributes())
                .isEqualTo("PEDESTAL_CODE=HR-BAY-P201\nPEDESTAL_ID=3\n");
    }

    @Test
    void eventIsGivenUpAfterTheLastAllowedAttempt() {
        EventRetry retry = EventRetry.of(new Event(EventType.BOOT_NOTIFICATION), "first", now);

        retry.failedAgain("second", now.plusMinutes(1), 2);
        assertThat(retry.getStatus()).isEqualTo(EventRetry.Status.OPEN);
        assertThat(retry.getRetryCount()).isEqualTo(1);

        retry.failedAgain("third", now.plusMinutes(2), 2);
        assertThat(retry.getStatus()).isEqualTo(EventRetry.Status.FAILED);
        assertThat(retry.getFailure()).isEqualTo("third");
        assertThat(retry.getLastRetryDate()).isEqualTo(now.plusMinutes(2));
    }

    @Test
    void sentEventCountsItsAttempt() {
        EventRetry retry = EventRetry.of(new Event(EventType.BOOT_NOTIFICATION), "first", now);

        retry.sent(now.plusMinutes(1));

        assertThat(retry.getStatus()).isEqualTo(EventRetry.Status.SENT);
        assertThat(retry.getRetryCount()).isEqualTo(1);
    }

    @Test
    void eventWithoutAttributesIsKeptAsEmptyTextAndReadBackEmpty() {
        EventRetry retry = EventRetry.of(new Event(EventType.BOOT_NOTIFICATION), "first", now);

        assertThat(retry.getAttributes()).isEmpty();
        assertThat(retry.toEvent().getAttributes()).isEmpty();
    }

    @Test
    void longFailureIsCut() {
        EventRetry retry = EventRetry.of(new Event(EventType.BOOT_NOTIFICATION), "x".repeat(2000), now);

        assertThat(retry.getFailure()).hasSize(EventRetry.MAX_FAILURE_LENGTH);
    }
}
