package io.pillopl.event;

import java.time.LocalDateTime;

/**
 * A tariff was assigned to a pedestal or to one of its sockets from a start date on (null tariff: planned
 * assignments were removed). Listeners: the start answer price text cached by the pedestal link, the guest app.
 */
public class TariffAppliedEvent extends Event {

    /**
     * @param socketId null for an assignment to the whole pedestal
     * @param tariffId null when planned assignments were removed
     */
    public TariffAppliedEvent(Long pedestalId, Long socketId, Long tariffId, LocalDateTime startDate) {
        super(EventType.TARIFF_APPLIED);
        setAttribute(EventAttribute.PEDESTAL_ID, pedestalId);
        setAttribute(EventAttribute.SOCKET_ID, socketId);
        setAttribute(EventAttribute.TARIFF_ID, tariffId);
        setAttribute(EventAttribute.START_TIME, startDate);
    }

    public static Long getPedestalId(Event event) {
        return EventAttribute.PEDESTAL_ID.getLongValue(event);
    }

    public static Long getSocketId(Event event) {
        return EventAttribute.SOCKET_ID.getLongValue(event);
    }

    public static Long getTariffId(Event event) {
        return EventAttribute.TARIFF_ID.getLongValue(event);
    }
}
