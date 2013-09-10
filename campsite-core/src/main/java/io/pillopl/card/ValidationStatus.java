package io.pillopl.card;

/**
 * Answer to the pedestal whether supply may start with a card at a socket.
 */
public enum ValidationStatus {
    ACCEPTED,
    /**
     * The socket cannot supply now (e.g. the power budget of its section is used up).
     */
    UNAVAILABLE,
    BLOCKED,
    EXPIRED,
    CARD_INVALID,
    SOCKET_INVALID,
    /**
     * The socket is already supplying for another card.
     */
    SOCKET_OCCUPIED,
    CARD_NOT_ACCEPTED_AT_SITE
}
