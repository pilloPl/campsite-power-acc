package io.pillopl.webservice.console.issuer;

import java.io.Serializable;

/**
 * Run the exchange with the issuer partners now.
 */
public record RunIssuerExchangeRequest(String authUsername) implements Serializable {
}
