package io.pillopl.card;

/**
 * STAFF: card of the campsite staff and pedestal service. RENTAL: card lent to a transit guest at the reception,
 * the consumption is settled by the campsite.
 */
public enum HouseCardType {
    STAFF,
    RENTAL;

    public static HouseCardType getByName(String name) {
        for (HouseCardType type : values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
