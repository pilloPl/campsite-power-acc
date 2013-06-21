package io.pillopl.webservice.console.commissioning;

import java.io.Serializable;

/**
 * The open commissioning the installer was given the token of.
 */
public record FindCommissioningByTokenRequest(String authUsername, String token) implements Serializable {
}
