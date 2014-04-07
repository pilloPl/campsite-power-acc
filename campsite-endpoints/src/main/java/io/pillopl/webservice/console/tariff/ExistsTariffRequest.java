package io.pillopl.webservice.console.tariff;

import java.io.Serializable;

/**
 * Checks whether a tariff name is taken in the caller's market.
 */
public record ExistsTariffRequest(String authUsername, String tariffName) implements Serializable {
}
