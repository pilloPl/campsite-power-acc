package io.pillopl.event;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventTest {

    @Test
    void attributeMustHaveTheClassOfItsKey() {
        Event event = new Event(EventType.METER_READING_CHANGED);

        event.setAttribute(EventAttribute.METER_VALUE, new BigDecimal("12.5"));

        assertThatThrownBy(() -> event.setAttribute(EventAttribute.PEDESTAL_ID, "42"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("PEDESTAL_ID must be of class Long");
    }

    @Test
    void typedGettersReadTheirOwnTypeOnly() {
        Event event = new Event(EventType.SUPPLY_STARTED)
                .setAttribute(EventAttribute.PEDESTAL_ID, 42L)
                .setAttribute(EventAttribute.SOCKET_NO, 2);

        assertThat(EventAttribute.PEDESTAL_ID.getLongValue(event)).isEqualTo(42L);
        assertThat(EventAttribute.SOCKET_NO.getIntegerValue(event)).isEqualTo(2);
        assertThat(EventAttribute.GROUP_ID.getLongValue(event)).isNull();
        assertThatThrownBy(() -> EventAttribute.PEDESTAL_ID.getIntegerValue(event))
                .isInstanceOf(ClassCastException.class);
    }

    @Test
    void eventsHaveTheirOwnIds() {
        assertThat(new Event(EventType.BOOT_NOTIFICATION).getId())
                .isNotEqualTo(new Event(EventType.BOOT_NOTIFICATION).getId());
    }
}
