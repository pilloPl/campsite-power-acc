package io.pillopl.card;

/**
 * Result of {@link CampingCard#validate}: the first reason why the card cannot be used, or VALID.
 */
public enum CardStatus {
    VALID,
    CARD_DEACTIVATED,
    NOT_VALID_YET,
    NOT_VALID_ANYMORE,
    NO_GUEST,
    GUEST_DEACTIVATED,
    BILLING_ACCOUNT_INVALID
}
