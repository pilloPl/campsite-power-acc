package io.pillopl.event;

/**
 * The default tariff of a campsite group (or, without group, of a market) changed.
 */
public class DefaultTariffChangedEvent extends Event {

    /**
     * @param groupId  null for the default tariff of the market
     * @param tariffId null when the campsite default was removed
     */
    public DefaultTariffChangedEvent(Long groupId, Long tariffId, String username) {
        super(EventType.DEFAULT_TARIFF_CHANGED);
        setAttribute(EventAttribute.GROUP_ID, groupId);
        setAttribute(EventAttribute.TARIFF_ID, tariffId);
        setAttribute(EventAttribute.USERNAME, username);
    }

    public static Long getGroupId(Event event) {
        return EventAttribute.GROUP_ID.getLongValue(event);
    }

    public static Long getTariffId(Event event) {
        return EventAttribute.TARIFF_ID.getLongValue(event);
    }
}
