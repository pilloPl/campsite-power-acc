package io.pillopl.group;

/**
 * The three typed trees of the organisation. A group and its parent always have the same type.
 */
public enum GroupType {

    /**
     * Card issuers: the network's own card, caravan clubs and discount card organisations with an agreement.
     */
    ISSUER,

    /**
     * Network, market, campsite, section (zone) of a campsite. Pedestals hang in this tree.
     */
    SITE,

    /**
     * Staff areas: campsite reception, market administration. Console users hang in this tree.
     */
    STAFF;

    public boolean isGroupType(Group group) {
        return group != null && name().equals(group.getType());
    }
}
