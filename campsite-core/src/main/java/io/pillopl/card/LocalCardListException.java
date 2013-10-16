package io.pillopl.card;

/**
 * A local card list could not be sent to a pedestal controller.
 */
public class LocalCardListException extends RuntimeException {

    private final String pedestalCode;

    public LocalCardListException(String pedestalCode, String message) {
        super(String.format("Local card list for %s not sent: %s", pedestalCode, message));
        this.pedestalCode = pedestalCode;
    }

    public String getPedestalCode() {
        return pedestalCode;
    }
}
