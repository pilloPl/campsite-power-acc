package io.pillopl.issuer;

/**
 * Synchronisation state of a card towards the card issuers (card list exchange): what the next export has to do
 * with the card. NOT and NEVER keep a card out of the exchange (NEVER for good).
 * <p>
 * A card carries two states: the state the card asks for now ({@code PARTNER_SYNC_STATE}) and the state the
 * partners got with the last export ({@code TARGET_PARTNER_SYNC_STATE}, null before the first export).
 * {@link #getByTargetPartnerSyncState(PartnerSyncState)} combines the wanted change with what the partners know.
 */
public enum PartnerSyncState {
    ADD,
    MODIFY,
    DELETE,
    UNKNOWN,
    NOT,
    NEVER;

    /**
     * The state to export when this change is wanted and the partners know the card in {@code targetSyncState}.
     * Kept as nested switches, one block per wanted change: an ADD of a card the partners have already is nothing
     * to do, a DELETE of a card they never got is nothing to do, a MODIFY of a card they do not have is an ADD.
     *
     * @param targetSyncState the state of the last export, null when the card was never exported
     */
    public PartnerSyncState getByTargetPartnerSyncState(PartnerSyncState targetSyncState) {
        if (targetSyncState == null) {
            switch (this) {
                case ADD:
                    return ADD;
                case DELETE:
                    return NOT;
                case MODIFY:
                    return ADD;
                case UNKNOWN:
                    return UNKNOWN;
                case NOT:
                    return NOT;
                case NEVER:
                    return NEVER;
                default:
                    return UNKNOWN;
            }
        }

        if (this == ADD) {
            switch (targetSyncState) {
                case ADD:
                    return NOT;
                case DELETE:
                    return ADD;
                case MODIFY:
                    return MODIFY;
                case UNKNOWN:
                    return UNKNOWN;
                case NOT:
                    return UNKNOWN;
                case NEVER:
                    return NEVER;
                default:
                    return UNKNOWN;
            }
        }

        if (this == DELETE) {
            switch (targetSyncState) {
                case ADD:
                    return DELETE;
                case DELETE:
                    return NOT;
                case MODIFY:
                    return DELETE;
                case UNKNOWN:
                    return UNKNOWN;
                case NOT:
                    return UNKNOWN;
                case NEVER:
                    return NEVER;
                default:
                    return UNKNOWN;
            }
        }

        if (this == MODIFY) {
            switch (targetSyncState) {
                case ADD:
                    return MODIFY;
                case DELETE:
                    return ADD;
                case MODIFY:
                    return MODIFY;
                case UNKNOWN:
                    return UNKNOWN;
                case NOT:
                    return UNKNOWN;
                case NEVER:
                    return NEVER;
                default:
                    return UNKNOWN;
            }
        }

        if (this == UNKNOWN) {
            switch (targetSyncState) {
                case ADD:
                case DELETE:
                case MODIFY:
                case UNKNOWN:
                case NOT:
                    return UNKNOWN;
                case NEVER:
                    return NEVER;
                default:
                    return UNKNOWN;
            }
        }

        if (this == NOT) {
            switch (targetSyncState) {
                case ADD:
                    return NOT;
                case DELETE:
                    return NOT;
                case MODIFY:
                    return NOT;
                case UNKNOWN:
                    return UNKNOWN;
                case NOT:
                    return NOT;
                case NEVER:
                    return NEVER;
                default:
                    return UNKNOWN;
            }
        }

        if (this == NEVER) {
            return NEVER;
        }

        return null;
    }

    /**
     * States the card list export sends to the partners.
     */
    public boolean isExported() {
        return this == ADD || this == MODIFY || this == DELETE;
    }
}
