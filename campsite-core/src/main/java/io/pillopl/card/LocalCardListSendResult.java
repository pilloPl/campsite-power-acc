package io.pillopl.card;

/**
 * Outcome of sending a local card list to a pedestal (or putting it into the pedestal's queue).
 */
public record LocalCardListSendResult(boolean success, String errorMessage) {

    public static LocalCardListSendResult ok() {
        return new LocalCardListSendResult(true, null);
    }

    public static LocalCardListSendResult failed(String errorMessage) {
        return new LocalCardListSendResult(false, errorMessage);
    }
}
