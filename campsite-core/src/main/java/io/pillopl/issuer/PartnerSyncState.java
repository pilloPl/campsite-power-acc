package io.pillopl.issuer;

/**
 * Synchronisation state of a card towards the card issuers (card list exchange): what the next export has to do
 * with the card. NOT and NEVER keep a card out of the exchange (NEVER for good).
 */
public enum PartnerSyncState {
    ADD,
    MODIFY,
    DELETE,
    UNKNOWN,
    NOT,
    NEVER
}
