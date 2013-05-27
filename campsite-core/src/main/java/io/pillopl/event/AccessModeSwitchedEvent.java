package io.pillopl.event;

/**
 * An affiliate campsite group was moved under the parent of another access mode; its pedestals got new flags.
 */
public class AccessModeSwitchedEvent extends Event {

    public AccessModeSwitchedEvent(Long affiliateGroupId, String accessMode) {
        super(EventType.ACCESS_MODE_SWITCHED);
        setAttribute(EventAttribute.GROUP_ID, affiliateGroupId);
        setAttribute(EventAttribute.ACCESS_MODE, accessMode);
    }

    public static Long getGroupId(Event event) {
        return EventAttribute.GROUP_ID.getLongValue(event);
    }

    public static String getAccessMode(Event event) {
        return EventAttribute.ACCESS_MODE.getStringValue(event);
    }
}
