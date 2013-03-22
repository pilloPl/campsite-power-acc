package io.pillopl.user;

/**
 * The groups of an affiliate owner are not set up the way commissioning expects.
 */
public class AffiliateSetupException extends RuntimeException {

    public AffiliateSetupException(String message) {
        super(message);
    }
}
