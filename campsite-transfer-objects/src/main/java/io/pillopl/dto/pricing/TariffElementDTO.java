package io.pillopl.dto.pricing;

import java.io.Serializable;
import java.util.List;

/**
 * An element of a tariff: its components and the optional restriction (null applies always).
 */
public record TariffElementDTO(Long id, int position, List<PriceComponentDTO> priceComponents,
                               TariffRestrictionDTO restriction) implements Serializable {
}
