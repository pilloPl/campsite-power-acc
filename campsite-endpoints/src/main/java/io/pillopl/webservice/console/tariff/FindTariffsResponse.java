package io.pillopl.webservice.console.tariff;

import io.pillopl.dto.pricing.TariffDTO;

import java.io.Serializable;
import java.util.List;

/**
 * The tariffs of the market, ordered by name.
 */
public record FindTariffsResponse(List<TariffDTO> tariffs) implements Serializable {
}
