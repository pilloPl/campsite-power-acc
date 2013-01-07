package io.pillopl.group;

/**
 * Market of a group, read from the name prefix of the group or of its nearest ancestor ("HR SITES", "IT STAFF"...).
 * {@link #UNKNOWN} never appears as a prefix; it is the fallback of callers that need a value.
 */
public enum Market {
    HR, IT, FR, UNKNOWN;

    public boolean isPrefixMarket() {
        return this != UNKNOWN;
    }
}
