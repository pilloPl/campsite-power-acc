package io.pillopl.group;

/**
 * Group names that carry logic. Market specific names are composed in {@link MarketExtension}.
 */
public final class GroupServiceConstants {

    /**
     * The network's own card (home market HR).
     */
    public static final String NETWORK_CARD = "NETWORK CARD";

    /**
     * Prefix of an affiliate campsite group: "AFFILIATE &lt;username&gt;" of the independent campsite owner.
     */
    public static final String PREFIX_AFFILIATE = "AFFILIATE";

    /**
     * Prefix of a pedestal stock group of a manufacturer: "SUPPLIER &lt;manufacturer&gt;".
     */
    public static final String PREFIX_SUPPLIER = "SUPPLIER";

    public static final String ISSUERS = "ISSUERS";

    public static final String SITES = "SITES";

    public static final String STAFF = "STAFF";

    public static final String RECEPTION = "RECEPTION";

    /**
     * Welcome cards shipped with an affiliate kit, adopted by the affiliate campsite on first use.
     */
    public static final String WELCOME_CARD_POOL = "WELCOME_CARD_POOL";

    public static final String NETWORK_ACCESS = AccessMode.NETWORK_ACCESS.getGroupNamePrefix();

    public static final String HOUSE_CARDS_ONLY = AccessMode.HOUSE_CARDS_ONLY.getGroupNamePrefix();

    private GroupServiceConstants() {
        throw new AssertionError();
    }
}
