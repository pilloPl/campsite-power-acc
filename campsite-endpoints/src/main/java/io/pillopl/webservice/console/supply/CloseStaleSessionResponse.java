package io.pillopl.webservice.console.supply;

import io.pillopl.dto.supply.SupplySessionDTO;

import java.io.Serializable;

/**
 * The session closed with TIMEOUT, with its consumption record.
 */
public record CloseStaleSessionResponse(SupplySessionDTO session) implements Serializable {
}
