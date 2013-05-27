package io.pillopl.event;

import io.pillopl.user.UserinfoChangeType;

/**
 * A console user was created or changed (data, groups or roles).
 */
public class UserinfoChangedEvent extends Event {

    public UserinfoChangedEvent(Long userId, String username, UserinfoChangeType changeType) {
        super(EventType.USERINFO_CHANGED);
        setAttribute(EventAttribute.ID, userId);
        setAttribute(EventAttribute.USERNAME, username);
        setAttribute(EventAttribute.CHANGE_TYPE, changeType.name());
    }

    public static UserinfoChangeType getChangeType(Event event) {
        String value = EventAttribute.CHANGE_TYPE.getStringValue(event);
        return value != null ? UserinfoChangeType.valueOf(value) : null;
    }
}
