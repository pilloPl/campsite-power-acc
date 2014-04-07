package io.pillopl.webservice.console.tariff;

import io.pillopl.dto.pricing.PriceSimulationDTO;

import java.io.Serializable;

/**
 * The simulated price; a supply that cannot be priced is answered with status PRICING_FAILED and the reason.
 */
public record SimulatePriceResponse(PriceSimulationDTO simulation) implements Serializable {
}
