package io.pillopl.dto.pricing;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A tariff of a market with its elements in evaluation order and its price text.
 */
public record TariffDTO(Long id, String name, String description, String market, String currency,
                        boolean defaultTariff, LocalDateTime lastUpdateTime, String priceText,
                        List<TariffElementDTO> elements) implements Serializable {
}
