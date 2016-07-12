package io.pillopl.webservice.console.tariff;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Assigns a tariff to whole pedestals from a start date on; no tariff removes the planned assignments.
 */
public record ApplyTariffToPedestalsRequest(List<String> pedestalCodes, Long tariffId,
                                            LocalDateTime startDate)
        implements Serializable {
}
