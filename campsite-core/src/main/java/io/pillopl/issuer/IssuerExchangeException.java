package io.pillopl.issuer;

/**
 * The exchange with an issuer partner failed: a line was refused (unknown card, wrong issuer, unknown action) or the
 * transport could not reach the partner or read its data.
 */
public class IssuerExchangeException extends RuntimeException {

    public IssuerExchangeException(String message) {
        super(message);
    }

    public IssuerExchangeException(String message, Throwable cause) {
        super(message, cause);
    }
}
