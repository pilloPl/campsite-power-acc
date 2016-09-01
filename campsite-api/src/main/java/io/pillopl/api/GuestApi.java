package io.pillopl.api;

import io.pillopl.dto.guest.GuestDTO;
import io.pillopl.dto.guest.GuestDetailDTO;
import io.pillopl.dto.guest.OrderDTO;
import io.pillopl.dto.guest.SeasonalContractDTO;
import io.pillopl.dto.guest.StayDTO;
import io.pillopl.dto.guest.UnitDTO;
import io.pillopl.value.CardId;
import io.pillopl.value.GroupId;
import io.pillopl.value.PitchNo;
import io.pillopl.value.Username;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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

    /**
     * Guests of the caller's ISSUER scope whose name, first name, plate or card number contains the text (all for
     * no text). GUEST_VIEW.
     */
    List<GuestDTO> findGuests(Username authUsername, String search);

    /**
     * A guest with units, cards, billing accounts, seasonal contracts, stays and orders. GUEST_VIEW.
     */
    GuestDetailDTO getGuest(Username authUsername, Long guestId);

    /**
     * Stays at the campsites of the caller's SITE scope overlapping the period. GUEST_VIEW.
     *
     * @param campsite null for all campsites of the scope
     * @param state    OPEN, CHECKED_OUT or null for all
     */
    List<StayDTO> findStays(Username authUsername, GroupId campsite, String state, LocalDate from, LocalDate to);

    /**
     * Seasonal contracts on pitches of the caller's SITE scope or of guests of its ISSUER scope. GUEST_VIEW.
     *
     * @param status ACTIVE, TERMINATION_REQUESTED, ENDED or null for all
     */
    List<SeasonalContractDTO> findSeasonalContracts(Username authUsername, String status);

    /**
     * A new guest in an ISSUER group of the caller's scope. GUEST_EDIT.
     */
    GuestDTO createGuest(Username authUsername, GroupId issuerGroup, GuestDTO details);

    /**
     * A camper, caravan or tent of the guest. GUEST_EDIT.
     */
    UnitDTO addUnit(Username authUsername, Long guestId, String unitType, String plate, BigDecimal lengthM);

    /**
     * The stay is extended or shortened; the card handed out for it follows. GUEST_EDIT.
     */
    StayDTO changeDeparture(Username authUsername, Long stayId, LocalDate departure);

    /**
     * Cancels an order (booking) not active yet. GUEST_EDIT.
     */
    OrderDTO cancelOrder(Username authUsername, Long orderId, String reason);
}
