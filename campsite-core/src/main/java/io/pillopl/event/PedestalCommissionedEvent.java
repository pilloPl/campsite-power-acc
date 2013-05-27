package io.pillopl.event;

/**
 * A pedestal was commissioned on an affiliate campsite (moved from the supplier stock into the campsite group).
 */
public class PedestalCommissionedEvent extends Event {

    public PedestalCommissionedEvent(Long pedestalId, String pedestalCode, Long groupId, String installerUsername) {
        super(EventType.PEDESTAL_COMMISSIONED);
        setAttribute(EventAttribute.PEDESTAL_ID, pedestalId);
        setAttribute(EventAttribute.PEDESTAL_CODE, pedestalCode);
        setAttribute(EventAttribute.GROUP_ID, groupId);
        setAttribute(EventAttribute.USERNAME, installerUsername);
    }

    public static Long getPedestalId(Event event) {
        return EventAttribute.PEDESTAL_ID.getLongValue(event);
    }

    public static Long getGroupId(Event event) {
        return EventAttribute.GROUP_ID.getLongValue(event);
    }
}
