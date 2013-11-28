package io.pillopl.card;

import io.pillopl.group.GroupService;
import io.pillopl.guest.Guest;
import io.pillopl.issuer.PartnerSyncState;
import io.pillopl.persistence.dao.CampingCardDao;
import io.pillopl.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CARD_UTILS.UPSERT_CARD: update the living card with the UID and number, otherwise insert it.
 */
@Transactional
class CardUtilsIntegrationTest extends IntegrationTest {

    @Autowired CampingCardDao campingCardDao;
    @Autowired GroupService groupService;
    @Autowired CardFixture cards;

    @Test
    void newCardIsInsertedWithTheDecimalUidOfTheReaders() {
        Guest guest = cards.guest("Kovac", groupService.fetchNetworkCardIssuerGroup());

        Long id = campingCardDao.upsertCard("04A1B2C3", "HR-77", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 14),
                "400012345678", guest.getId());

        CampingCard card = campingCardDao.findById(id).orElseThrow();
        assertThat(card.getCardRfid()).isEqualTo(RfidConverter.hexBigEndianToDecimal("04A1B2C3"));
        assertThat(card.getGuest().getId()).isEqualTo(guest.getId());
        assertThat(card.getValidTo()).isEqualTo(LocalDate.of(2026, 7, 14));
        assertThat(card.getPartnerSyncState()).isEqualTo(PartnerSyncState.UNKNOWN);
        assertThat(card.getActivation()).isEqualTo(CardActivation.ACTIVATED);
    }

    @Test
    void livingCardWithTheSameUidAndNumberIsUpdatedAndReactivated() {
        Guest first = cards.guest("Kovac", groupService.fetchNetworkCardIssuerGroup());
        Guest second = cards.guest("Babic", groupService.fetchNetworkCardIssuerGroup());
        String decimal = RfidConverter.hexBigEndianToDecimal("0A0B0C0D");
        CampingCard existing = cards.card(decimal, "HR-78", first, null, null);
        existing.setActivation(CardActivation.DEACTIVATED);
        campingCardDao.saveAndFlush(existing);

        Long id = campingCardDao.upsertCard("0A0B0C0D", "HR-78", null, LocalDate.of(2026, 10, 1), "500012345678",
                second.getId());

        assertThat(id).isEqualTo(existing.getId());
        CampingCard card = campingCardDao.findById(id).orElseThrow();
        assertThat(card.getGuest().getId()).isEqualTo(second.getId());
        assertThat(card.getBillingAccountNo()).isEqualTo("500012345678");
        assertThat(card.getActivation()).isEqualTo(CardActivation.ACTIVATED);
        assertThat(campingCardDao.findNotDeletedByCardNumber("HR-78")).hasSize(1);
    }

    @Test
    void deletedCardIsNotRevivedButReplaced() {
        Guest guest = cards.guest("Kovac", groupService.fetchNetworkCardIssuerGroup());
        CampingCard deleted = cards.card(RfidConverter.hexBigEndianToDecimal("01020304"), "HR-79", guest, null, null);
        deleted.setDeleted(true);
        campingCardDao.saveAndFlush(deleted);

        Long id = campingCardDao.upsertCard("01020304", "HR-79", null, null, "400012345678", guest.getId());

        assertThat(id).isNotEqualTo(deleted.getId());
    }
}
