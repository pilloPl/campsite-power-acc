package io.pillopl.webservice.console.tariff;

import java.io.Serializable;

/**
 * Sets the default tariff of a campsite or section group; no tariff removes it.
 */
public record SetCampsiteDefaultTariffRequest(Long groupId, Long tariffId)
        implements Serializable {
}
