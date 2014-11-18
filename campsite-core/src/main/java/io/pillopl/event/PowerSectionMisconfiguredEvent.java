package io.pillopl.event;

/**
 * Alert: the budget of a section switchboard could not be checked (no algorithm, unknown bean, failing algorithm),
 * so supplies at its sockets are refused until the configuration is fixed.
 */
public class PowerSectionMisconfiguredEvent extends Event {

    public PowerSectionMisconfiguredEvent(Long powerSectionId, Long socketId, String pedestalCode, Integer socketNo,
                                          String problem) {
        super(EventType.POWER_SECTION_MISCONFIGURED);
        setAttribute(EventAttribute.POWER_SECTION_ID, powerSectionId);
        setAttribute(EventAttribute.SOCKET_ID, socketId);
        setAttribute(EventAttribute.PEDESTAL_CODE, pedestalCode);
        setAttribute(EventAttribute.SOCKET_NO, socketNo);
        setAttribute(EventAttribute.STATUS, problem);
    }

    public static Long getPowerSectionId(Event event) {
        return EventAttribute.POWER_SECTION_ID.getLongValue(event);
    }

    public static String getProblem(Event event) {
        return EventAttribute.STATUS.getStringValue(event);
    }
}
