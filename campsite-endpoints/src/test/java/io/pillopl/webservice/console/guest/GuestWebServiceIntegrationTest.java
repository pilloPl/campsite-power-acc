package io.pillopl.webservice.console.guest;

import io.pillopl.card.CampingCard;
import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupType;
import io.pillopl.guest.Guest;
import io.pillopl.guest.GuestType;
import io.pillopl.persistence.dao.CampingCardDao;
import io.pillopl.persistence.dao.GuestDao;
import io.pillopl.user.UserroleName;
import io.pillopl.webservice.console.WebServiceIntegrationTest;
import io.pillopl.webservice.console.common.WebServicePaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GuestWebServiceIntegrationTest extends WebServiceIntegrationTest {

    static final String PATH = WebServicePaths.GUEST;
    static final LocalDate ARRIVAL = LocalDate.now();
    static final LocalDate DEPARTURE = ARRIVAL.plusDays(5);

    @Autowired GroupManager groupManager;
    @Autowired GuestDao guestDao;
    @Autowired CampingCardDao campingCardDao;

    Group bay;
    Guest guest;
    CampingCard card;

    @BeforeEach
    void setUp() {
        Group club = groupManager.createChildGroup(console.issuer("HR ISSUERS"), "GUEST CONSOLE CLUB", "Club",
                GroupType.ISSUER);
        bay = groupManager.createChildGroup(console.site("HR SITES"), "GUEST CONSOLE BAY", "Bay", GroupType.SITE);
        guest = guestDao.saveAndFlush(new Guest.Builder(club, GuestType.TRANSIT, "Kovac").carPlate("ZG 1 AB")
                .build());
        card = campingCardDao.saveAndFlush(new CampingCard.Builder("7401", "HR-7401").guest(guest)
                .billingAccountNo("400012345678").build());
        console.user("desk", UserroleName.RECEPTION, console.staff("HR RECEPTION"), club, bay);
        console.user("service", UserroleName.CUSTOMER_SERVICE, console.staff("HR RECEPTION"), club, bay);
        console.user("otherdesk", UserroleName.RECEPTION, console.staff("HR RECEPTION"), bay);
        console.user("technician", UserroleName.SERVICE_TECHNICIAN, console.staff("HR RECEPTION"), club, bay);
    }

    CheckInRequest checkIn() {
        return new CheckInRequest(guest.getId(), bay.getId(), "a12", ARRIVAL, DEPARTURE, card.getId(),
                null, "400012345678");
    }

    @Test
    void receptionChecksTheGuestInWithTheCardValidForTheStay() throws Exception {
        call("desk", PATH + "/checkIn", checkIn())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stay.pitchNo").value("A12"))
                .andExpect(jsonPath("$.stay.campsiteName").value("Bay"))
                .andExpect(jsonPath("$.stay.cardNumber").value("HR-7401"))
                .andExpect(jsonPath("$.stay.cardValidTo").value(DEPARTURE.toString()))
                .andExpect(jsonPath("$.stay.unitPlate").value("ZG1AB"))
                .andExpect(jsonPath("$.stay.nights").value(5))
                .andExpect(jsonPath("$.stay.checkedInBy").value("desk"));
    }

    @Test
    void receptionChecksTheStayOutAndTheCardEndsToday() throws Exception {
        String body = call("desk", PATH + "/checkIn", checkIn()).andReturn().getResponse().getContentAsString();
        Long stayId = objectMapper.readTree(body).path("stay").path("id").asLong();

        call("desk", PATH + "/checkOut", new CheckOutRequest(stayId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stay.checkedOutBy").value("desk"))
                .andExpect(jsonPath("$.stay.cardValidTo").value(LocalDate.now().toString()));
        call("desk", PATH + "/checkOut", new CheckOutRequest(stayId)).andExpect(status().isConflict());
    }

    @Test
    void checkOutNeedsItsOwnRight() throws Exception {
        String body = call("service", PATH + "/checkIn", checkIn()).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString();
        Long stayId = objectMapper.readTree(body).path("stay").path("id").asLong();

        call("service", PATH + "/checkOut", new CheckOutRequest(stayId)).andExpect(status().isForbidden());
    }

    @Test
    void checkInIsRefusedForGuestsOutsideTheScopeIncompleteRequestsAndOccupiedPitches() throws Exception {
        call("otherdesk", PATH + "/checkIn", checkIn()).andExpect(status().isForbidden());
        call("desk", PATH + "/checkIn", new CheckInRequest(guest.getId(), bay.getId(), "A12", null, DEPARTURE, null,
                null, null)).andExpect(status().isBadRequest());
        call("desk", PATH + "/checkIn", new CheckInRequest(999_999L, bay.getId(), "A12", ARRIVAL, DEPARTURE, null,
                null, null)).andExpect(status().isNotFound());
        call("desk", PATH + "/checkIn", checkIn()).andExpect(status().isOk());
        call("desk", PATH + "/checkIn", new CheckInRequest(guest.getId(), bay.getId(), "A12", ARRIVAL, DEPARTURE,
                null, null, null)).andExpect(status().isConflict());
    }

    @Test
    void receptionFindsGuestsOfItsIssuersByNamePlateOrCardNumber() throws Exception {
        call("desk", PATH + "/findGuests", new FindGuestsRequest("kov"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guests.length()").value(1))
                .andExpect(jsonPath("$.guests[0].name").value("Kovac"))
                .andExpect(jsonPath("$.guests[0].issuerName").value("Club"));
        call("desk", PATH + "/findGuests", new FindGuestsRequest("HR-7401"))
                .andExpect(jsonPath("$.guests[0].id").value(guest.getId()));
        call("desk", PATH + "/findGuests", new FindGuestsRequest("zg 1"))
                .andExpect(jsonPath("$.guests.length()").value(1));
        call("otherdesk", PATH + "/findGuests", new FindGuestsRequest(null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guests.length()").value(0));
    }

    @Test
    void guestPageShowsUnitsCardsAndStays() throws Exception {
        call("desk", PATH + "/addUnit", new AddUnitRequest(guest.getId(), "caravan", "zg 2 cd",
                new java.math.BigDecimal("7.5")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unit.unitType").value("CARAVAN"));
        call("desk", PATH + "/checkIn", checkIn()).andExpect(status().isOk());

        call("desk", PATH + "/getGuest", new GetGuestRequest(guest.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guest.guest.name").value("Kovac"))
                .andExpect(jsonPath("$.guest.units.length()").value(1))
                .andExpect(jsonPath("$.guest.cards[0].cardNumber").value("HR-7401"))
                .andExpect(jsonPath("$.guest.stays[0].pitchNo").value("A12"))
                .andExpect(jsonPath("$.guest.billingAccounts[0].erpAccountNo").value("400012345678"));
        call("otherdesk", PATH + "/getGuest", new GetGuestRequest(guest.getId())).andExpect(status().isForbidden());
        call("desk", PATH + "/getGuest", new GetGuestRequest(999_999L)).andExpect(status().isNotFound());
    }

    @Test
    void staysOfTheCampsiteAreFoundByStateAndPeriodAndTheDepartureCanChange() throws Exception {
        String body = call("desk", PATH + "/checkIn", checkIn()).andReturn().getResponse().getContentAsString();
        Long stayId = objectMapper.readTree(body).path("stay").path("id").asLong();

        call("desk", PATH + "/findStays", new FindStaysRequest(bay.getId(), "open", ARRIVAL, ARRIVAL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stays.length()").value(1))
                .andExpect(jsonPath("$.stays[0].id").value(stayId));
        call("desk", PATH + "/findStays", new FindStaysRequest(null, "CHECKED_OUT", ARRIVAL, DEPARTURE))
                .andExpect(jsonPath("$.stays.length()").value(0));
        call("desk", PATH + "/findStays", new FindStaysRequest(null, null, DEPARTURE.plusDays(1),
                DEPARTURE.plusDays(3))).andExpect(jsonPath("$.stays.length()").value(0));
        call("desk", PATH + "/findStays", new FindStaysRequest(null, "SOMETIMES", ARRIVAL, DEPARTURE))
                .andExpect(status().isBadRequest());
        call("desk", PATH + "/findStays", new FindStaysRequest(console.site("IT SITES").getId(), null, ARRIVAL,
                DEPARTURE)).andExpect(status().isForbidden());

        call("desk", PATH + "/changeDeparture", new ChangeDepartureRequest(stayId, DEPARTURE.plusDays(2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stay.departureDate").value(DEPARTURE.plusDays(2).toString()))
                .andExpect(jsonPath("$.stay.cardValidTo").value(DEPARTURE.plusDays(2).toString()));
        call("desk", PATH + "/changeDeparture", new ChangeDepartureRequest(stayId, ARRIVAL.minusDays(1)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void receptionCreatesAGuestInItsIssuerGroupOnly() throws Exception {
        Long clubId = guest.getGroup().getId();
        call("desk", PATH + "/createGuest", new CreateGuestRequest(clubId, "seasonal", "Novak", "Ana", null, null,
                "Rijeka", "HR", "RI 77 X", null, "invoice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guest.guestType").value("SEASONAL"))
                .andExpect(jsonPath("$.guest.paymentMethod").value("INVOICE"))
                .andExpect(jsonPath("$.guest.issuerName").value("Club"));
        call("otherdesk", PATH + "/createGuest", new CreateGuestRequest(clubId, null, "Novak", null, null, null,
                null, null, null, null, null)).andExpect(status().isForbidden());
        call("desk", PATH + "/createGuest", new CreateGuestRequest(bay.getId(), null, "Novak", null, null, null,
                null, null, null, null, null)).andExpect(status().isBadRequest());
        call("technician", PATH + "/findGuests", new FindGuestsRequest(null)).andExpect(status().isForbidden());
    }

    @Test
    void seasonalContractsAndUnknownOrdersOfTheScope() throws Exception {
        call("desk", PATH + "/findSeasonalContracts", new FindSeasonalContractsRequest(null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seasonalContracts.length()").value(0));
        call("desk", PATH + "/findSeasonalContracts", new FindSeasonalContractsRequest("FOREVER"))
                .andExpect(status().isBadRequest());
        call("desk", PATH + "/cancelOrder", new CancelOrderRequest(999_999L, null)).andExpect(status().isNotFound());
    }
}
