package io.pillopl.card;

import io.pillopl.group.Group;
import io.pillopl.guest.Guest;
import io.pillopl.guest.GuestType;
import io.pillopl.persistence.dao.CampingCardDao;
import io.pillopl.persistence.dao.GuestDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Builds and saves guests and their camping cards for integration tests.
 */
@Component
public class CardFixture {

    public static final String BILLING_ACCOUNT_NO = "400012345678";

    @Autowired
    GuestDao guestDao;

    @Autowired
    CampingCardDao campingCardDao;

    public Guest guest(String name, Group issuerGroup) {
        return guestDao.saveAndFlush(new Guest(issuerGroup, GuestType.TRANSIT, name));
    }

    public CampingCard card(String rfid, Guest guest) {
        return card(rfid, "N" + rfid, guest, LocalDate.now().minusDays(2), LocalDate.now().plusDays(5));
    }

    public CampingCard card(String rfid, String number, Guest guest, LocalDate validFrom, LocalDate validTo) {
        return campingCardDao.saveAndFlush(new CampingCard.Builder(rfid, number)
                .guest(guest)
                .validFrom(validFrom)
                .validTo(validTo)
                .billingAccountNo(BILLING_ACCOUNT_NO)
                .build());
    }
}
