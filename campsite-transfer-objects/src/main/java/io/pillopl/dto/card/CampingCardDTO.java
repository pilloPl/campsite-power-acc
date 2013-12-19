package io.pillopl.dto.card;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * A camping card with its guest, validity and the result of its validation today.
 */
public record CampingCardDTO(Long id, String cardRfid, String cardNumber, String activation, Long guestId,
                             String guestName, String issuerGroupName, LocalDate validFrom, LocalDate validTo,
                             String billingAccountNo, String partnerSyncState, String cardStatus)
        implements Serializable {
}
