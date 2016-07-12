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
}
