package io.pillopl.webservice.console.guest;

import java.io.Serializable;

/**
 * A new guest in an issuer group of the caller (ISSUER group id); type TRANSIT (default) or SEASONAL,
 * payment method RECEPTION, DIRECT_DEBIT or INVOICE.
 */
public record CreateGuestRequest(Long issuerGroupId, String guestType, String name, String firstName, String email, String phone,
                                 String city, String countryIso, String carPlate, String trailerPlate,
                                 String paymentMethod) implements Serializable {
}
