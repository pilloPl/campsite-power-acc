package io.pillopl.event;

/**
 * A pedestal went back from an affiliate campsite to the supplier stock.
 */
public class PedestalDecommissionedEvent extends Event {

    public PedestalDecommissionedEvent(Long pedestalId, String pedestalCode, Long supplierGroupId, String username) {
        super(EventType.PEDESTAL_DECOMMISSIONED);
        setAttribute(EventAttribute.PEDESTAL_ID, pedestalId);
        setAttribute(EventAttribute.PEDESTAL_CODE, pedestalCode);
        setAttribute(EventAttribute.GROUP_ID, supplierGroupId);
        setAttribute(EventAttribute.USERNAME, username);
    }

    public static Long getPedestalId(Event event) {
        return EventAttribute.PEDESTAL_ID.getLongValue(event);
    }
}
