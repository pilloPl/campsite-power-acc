package io.pillopl.guest;

import io.pillopl.api.GuestApi;
import io.pillopl.api.exception.InvalidInputException;
import io.pillopl.card.CampingCard;
import io.pillopl.dto.guest.StayDTO;
import io.pillopl.user.PrincipalResolver;
import io.pillopl.user.Userinfo;
import io.pillopl.user.UserrightName;
import io.pillopl.util.ApiExceptionTranslator;
import io.pillopl.value.CardId;
import io.pillopl.value.GroupId;
import io.pillopl.value.PitchNo;
import io.pillopl.value.Username;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Stay calls of the console: GUEST_EDIT to check a guest in, STAY_CHECKOUT to check a stay out.
 */
@Service
@Transactional
public class GuestApiService implements GuestApi {

    private final GuestService guestService;

    private final PrincipalResolver principalResolver;

    public GuestApiService(GuestService guestService, PrincipalResolver principalResolver) {
        this.guestService = guestService;
        this.principalResolver = principalResolver;
    }

    @Override
    public StayDTO checkIn(Username authUsername, Long guestId, GroupId campsite, PitchNo pitchNo, LocalDate arrival,
                           LocalDate departure, CardId cardId, Long unitId, String billingAccountNo) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.GUEST_EDIT);
            if (guestId == null || campsite == null) {
                throw new InvalidInputException("Guest and campsite of the stay are missing");
            }
            if (arrival == null || departure == null) {
                throw new InvalidInputException("Arrival and departure of the stay are missing");
            }
            Stay stay = guestService.checkIn(principal, guestId, campsite.value(),
                    pitchNo != null ? pitchNo.value() : null, arrival, departure,
                    cardId != null ? cardId.value() : null, unitId, billingAccountNo);
            return toDTO(stay);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public StayDTO checkOut(Username authUsername, Long stayId) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.STAY_CHECKOUT);
            if (stayId == null) {
                throw new InvalidInputException("Stay id is missing");
            }
            return toDTO(guestService.checkOut(principal, stayId));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    static StayDTO toDTO(Stay stay) {
        CampingCard card = stay.getCard();
        return new StayDTO(stay.getId(), stay.getGuest().getId(), stay.getGuest().getFullName(),
                stay.getSiteGroup().getId(), stay.getSiteGroup().getDisplayName(), stay.getPitchNo(),
                card != null ? card.getId() : null, card != null ? card.getCardNumber() : null,
                card != null ? card.getValidTo() : null, card != null ? card.getBillingAccountNo() : null,
                stay.getUnitPlate(), stay.getArrivalDate(), stay.getDepartureDate(), stay.getNights(),
                stay.getCheckInDate(), stay.getCheckedInBy(), stay.getCheckOutDate(), stay.getCheckedOutBy());
    }
}
