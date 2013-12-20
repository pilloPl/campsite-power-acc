package io.pillopl.webservice.console.tariff;

import io.pillopl.dto.pricing.SocketTariffDTO;

import java.io.Serializable;

/**
 * The resolved tariff, its source and the assignment history of the socket.
 */
public record FindSocketTariffResponse(SocketTariffDTO socketTariff) implements Serializable {
}
