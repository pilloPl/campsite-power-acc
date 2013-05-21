package io.pillopl.event;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class EventServiceTest {

    private final EventService eventService = new EventService();

    @Test
    void eventGoesToEveryQueueRegisteredForItsType() {
        eventService.registerQueue("a", EventType.SUPPLY_STOPPED);
        eventService.registerQueue("b", EventType.SUPPLY_STOPPED);
        eventService.registerQueue("c", EventType.BOOT_NOTIFICATION);

        eventService.send(new Event(EventType.SUPPLY_STOPPED));

        assertThat(eventService.getQueueSize("a")).isEqualTo(1);
        assertThat(eventService.getQueueSize("b")).isEqualTo(1);
        assertThat(eventService.getQueueSize("c")).isZero();
    }

    @Test
    void eventWithoutListenerIsDropped() {
        eventService.send(new Event(EventType.CHECK_URL));

        assertThat(eventService.getQueueSize(EventService.createEventSystemName("x"))).isZero();
    }

    @Test
    void listenerHandlesTheEventsOfItsTypeOnItsOwnThreadAndTasks() throws Exception {
        List<String> handled = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(2);
        EventListener listener = new EventListener(eventService, new EventTaskExecutorFactory()) {
            @Override
            public String getName() {
                return "test-listener";
            }

            @Override
            public EventType getType() {
                return EventType.SOCKET_STATE_NOTIFICATION;
            }

            @Override
            public void handleEvent(Event event) {
                submitTask(new EventTask(event) {
                    @Override
                    public void handleEvent() {
                        handled.add(EventAttribute.PEDESTAL_CODE.getStringValue(event));
                        latch.countDown();
                    }
                });
            }
        };
        listener.postcreate();
        try {
            eventService.send(new Event(EventType.SOCKET_STATE_NOTIFICATION)
                    .setAttribute(EventAttribute.PEDESTAL_CODE, "P1"));
            eventService.send(new Event(EventType.BOOT_NOTIFICATION).setAttribute(EventAttribute.PEDESTAL_CODE, "X"));
            eventService.send(new Event(EventType.SOCKET_STATE_NOTIFICATION)
                    .setAttribute(EventAttribute.PEDESTAL_CODE, "P2"));

            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(handled).containsExactly("P1", "P2");
        } finally {
            listener.tryShutdown();
        }
    }

    @Test
    void failingTaskDoesNotStopTheListener() {
        EventTask failing = new EventTask(new Event(EventType.AUTHORIZATION)) {
            @Override
            public void handleEvent() {
                throw new IllegalStateException("boom");
            }
        };

        failing.run();

        assertThat(failing.getEvent().getType()).isEqualTo(EventType.AUTHORIZATION);
    }
}
