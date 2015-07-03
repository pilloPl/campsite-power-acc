package io.pillopl.dto.guest;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A stay as the reception sees it: guest, campsite and pitch, the card handed out with its validity, the unit and
 * the check-in and check-out.
 */
public record StayDTO(Long id, Long guestId, String guestName, Long siteGroupId, String campsiteName, String pitchNo,
                      Long cardId, String cardNumber, LocalDate cardValidTo, String billingAccountNo,
                      String unitPlate, LocalDate arrivalDate, LocalDate departureDate, long nights,
                      LocalDateTime checkInDate, String checkedInBy, LocalDateTime checkOutDate,
                      String checkedOutBy) implements Serializable {

    public boolean checkedOut() {
        return checkOutDate != null;
    }
}
