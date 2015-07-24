package io.pillopl.api;

import io.pillopl.dto.guest.StayDTO;
import io.pillopl.value.CardId;
import io.pillopl.value.GroupId;
import io.pillopl.value.PitchNo;
import io.pillopl.value.Username;

import java.time.LocalDate;

/**
 * Stays of the guests at the reception: check-in hands out the card valid for the stay, check-out ends it and lets
 * the open supply sessions of the stay be closed.
 */
public interface GuestApi {

    /**
     * @param pitchNo          null for a free choice of pitch
     * @param cardId           null for a stay without electricity
     * @param unitId           null for the guest's only unit with a plate
     * @param billingAccountNo null to keep the card's billing account
     */
    StayDTO checkIn(Username authUsername, Long guestId, GroupId campsite, PitchNo pitchNo, LocalDate arrival,
                    LocalDate departure, CardId cardId, Long unitId, String billingAccountNo);

    StayDTO checkOut(Username authUsername, Long stayId);
}
