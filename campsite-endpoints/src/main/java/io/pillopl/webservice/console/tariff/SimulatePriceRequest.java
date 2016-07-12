package io.pillopl.webservice.console.tariff;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Prices a supply on a socket as it would be charged, with the socket's tariff or a given one.
 */
public record SimulatePriceRequest(Long socketId, Long tariffId, LocalDateTime plugIn,
                                   LocalDateTime plugOut, BigDecimal kwh, Integer nights)
        implements Serializable {
}
