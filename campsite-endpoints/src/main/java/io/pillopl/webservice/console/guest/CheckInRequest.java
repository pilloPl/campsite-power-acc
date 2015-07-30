package io.pillopl.webservice.console.guest;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Check-in of a guest on a pitch of a campsite (SITE group id) from arrival to departure, optionally with the card
 * handed out, the unit and the billing account of the card.
 */
public record CheckInRequest(String authUsername, Long guestId, Long campsiteGroupId, String pitchNo,
                             LocalDate arrival, LocalDate departure, Long cardId, Long unitId,
                             String billingAccountNo) implements Serializable {
}
