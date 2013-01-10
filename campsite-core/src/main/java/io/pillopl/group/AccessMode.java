package io.pillopl.group;

/**
 * Access mode of an affiliate campsite, decided only by the name of its parent group.
 */
public enum AccessMode {

    /**
     * Pedestals open to network cards (and the cards accepted by agreement).
     */
    NETWORK_ACCESS("NETWORK ACCESS"),

    /**
     * Pedestals accept only the campsite's own house cards.
     */
    HOUSE_CARDS_ONLY("HOUSE CARDS ONLY");

    private final String groupNamePrefix;

    AccessMode(String groupNamePrefix) {
        this.groupNamePrefix = groupNamePrefix;
    }

    public String getGroupNamePrefix() {
        return groupNamePrefix;
    }
}
