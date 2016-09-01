package io.pillopl.dto.guest;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A guest in the list of the reception: issuer group, type (TRANSIT / SEASONAL), contact and plates.
 */
public record GuestDTO(Long id, Long issuerGroupId, String issuerName, String guestType, String name,
                       String firstName, String fullName, String email, String phone, String city, String countryIso,
                       String status, String erpPartnerNo, String carPlate, String trailerPlate,
                       String paymentMethod, LocalDateTime createDate) implements Serializable {
}
