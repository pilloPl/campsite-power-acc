package io.pillopl.dto.card;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Data of a card issued at the reception: the guest, the UID from the reader, the printed number, the stay or season
 * and the billing account.
 */
public record IssueCardDTO(Long guestId, String cardRfid, String cardNumber, LocalDate validFrom, LocalDate validTo,
                           String billingAccountNo) implements Serializable {
}
