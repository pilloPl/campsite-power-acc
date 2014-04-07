package io.pillopl.persistence.dao;

import io.pillopl.pricing.TariffApplicability;

import java.time.LocalDateTime;

public interface TariffApplicabilityDaoCustom {

    /**
     * @return the assignment of the socket with the newest start date not after {@code at}, or null
     */
    TariffApplicability findActiveTariffForSocket(Long socketId, LocalDateTime at);

    /**
     * @return the assignment of the whole pedestal with the newest start date not after {@code at}, or null
     */
    TariffApplicability findActiveTariffForPedestal(Long pedestalId, LocalDateTime at);
}
