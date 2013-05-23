package io.pillopl.dto.infrastructure;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Address of a campsite (or of a single pedestal) with its reception hours.
 */
public record LocationDTO(Long id, String name, String campsiteCode, String street, String houseNumber,
                          String zipcode, String city, String region, String countryIso, BigDecimal latitude,
                          BigDecimal longitude, String note, List<ReceptionHoursDTO> receptionHours)
        implements Serializable {

    public LocationDTO {
        receptionHours = receptionHours != null ? List.copyOf(receptionHours) : List.of();
    }
}
