package io.pillopl.webservice.console.tariff;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Assigns a tariff to all pedestals of an affiliate campsite owner from a start date on.
 */
public record ApplyTariffToAffiliatePedestalsRequest(String authUsername, String ownerUsername, Long tariffId,
                                                     LocalDateTime startDate)
        implements Serializable {
}
