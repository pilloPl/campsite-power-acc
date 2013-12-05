package io.pillopl.dto.pricing;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * One priced component of a simulated supply; the amount is shown in cents.
 */
public record PriceSimulationLineDTO(int elementPosition, String dimensionType, BigDecimal price, String priceUnit,
                                     BigDecimal share, BigDecimal billedQuantity, BigDecimal amount)
        implements Serializable {
}
