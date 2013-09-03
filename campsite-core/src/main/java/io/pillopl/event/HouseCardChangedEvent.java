package io.pillopl.event;

/**
 * A house card was created, changed, activated, deactivated or deleted: the local card lists of the pedestals of its
 * campsite need the change.
 */
public class HouseCardChangedEvent extends Event {

    public HouseCardChangedEvent(Long houseCardId) {
        super(EventType.HOUSE_CARD_CHANGED);
        setAttribute(EventAttribute.ID, houseCardId);
    }

    public static Long getHouseCardId(Event event) {
        return EventAttribute.ID.getLongValue(event);
    }
}
