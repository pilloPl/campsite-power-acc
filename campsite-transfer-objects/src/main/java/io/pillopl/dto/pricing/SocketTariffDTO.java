package io.pillopl.dto.pricing;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The tariff that prices supplies on a socket at a time, the step of the resolution that found it
 * (SEASONAL_CONTRACT, SOCKET, PEDESTAL, CAMPSITE_DEFAULT, MARKET_DEFAULT) and the assignment history.
 */
public record SocketTariffDTO(Long socketId, String socketCode, String pitchNo, LocalDateTime at, String source,
                              TariffDTO tariff, List<TariffApplicabilityDTO> assignments) implements Serializable {
}
