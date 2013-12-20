package io.pillopl.webservice.console.tariff;

import java.io.Serializable;

/**
 * Whether the name is taken.
 */
public record ExistsTariffResponse(String tariffName, boolean exists) implements Serializable {
}
