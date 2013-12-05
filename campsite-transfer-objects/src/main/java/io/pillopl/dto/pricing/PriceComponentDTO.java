package io.pillopl.dto.pricing;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * A price component of a tariff element: dimension (ENERGY, TIME, PER_NIGHT, FLAT), price per unit and step size.
 */
public record PriceComponentDTO(Long id, String dimensionType, BigDecimal price, Integer stepSize, String priceUnit)
        implements Serializable {
}
