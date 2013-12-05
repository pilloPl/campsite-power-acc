package io.pillopl.dto.pricing;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Price of a supply as it would be charged: status PRICED / PRICING_FAILED / NOT_BILLABLE, the cost (null when
 * failed) and the lines per component.
 */
public record PriceSimulationDTO(String status, BigDecimal cost, String currency, String costText, Long tariffId,
                                 String tariffName, String tariffSource, String failureReason,
                                 List<PriceSimulationLineDTO> lines) implements Serializable {
}
