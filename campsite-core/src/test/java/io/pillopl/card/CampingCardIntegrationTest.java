package io.pillopl.card;

import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.guest.Guest;
import io.pillopl.guest.GuestStatus;
import io.pillopl.issuer.PartnerSyncState;
import io.pillopl.persistence.dao.CampingCardDao;
import io.pillopl.persistence.dao.GuestDao;
import io.pillopl.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class CampingCardIntegrationTest extends IntegrationTest {

    @Autowired CampingCardDao campingCardDao;
    @Autowired GuestDao guestDao;
    @Autowired GroupService groupService;
    @Autowired CardFixture fixture;

    Guest guest;

    @BeforeEach
    void setUp() {
        Group networkCard = groupService.fetchNetworkCardIssuerGroup();
        guest = fixture.guest("Horvat", networkCard);
    }

    @Test
    void cardIsStoredWithItsGuestValidityAndSyncState() {
        CampingCard card = fixture.card("3054198966", "HR-000123", guest, LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 14));

        CampingCard found = campingCardDao.findByCardRfid("3054198966");

        assertThat(found).isEqualTo(card);
        assertThat(found.getGuest()).isEqualTo(guest);
        assertThat(found.getGuest().getStatus()).isEqualTo(GuestStatus.ACTIVE);
        assertThat(found.getValidTo()).isEqualTo(LocalDate.of(2026, 7, 14));
        assertThat(found.getActivation()).isEqualTo(CardActivation.ACTIVATED);
        assertThat(found.getPartnerSyncState()).isEqualTo(PartnerSyncState.ADD);
        assertThat(campingCardDao.findByGuestId(guest.getId())).containsExactly(card);
        assertThat(guestDao.findByGroupIds(List.of(guest.getGroup().getId()))).contains(guest);
    }

    @Test
    void theSameUidAndNumberCanLiveOnlyOnce() {
        fixture.card("1001", "HR-1001", guest, null, null);

        assertThatThrownBy(() -> fixture.card("1001", "HR-1001", guest, null, null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void aDeletedCardFreesItsUidForANewCard() {
        CampingCard old = fixture.card("1002", "HR-1002", guest, null, null);
        old.setDeleted(true);
        campingCardDao.saveAndFlush(old);

        CampingCard renewed = fixture.card("1002", "HR-1002", guest, null, null);

        assertThat(campingCardDao.findByCardRfid("1002")).isEqualTo(renewed);
        assertThat(campingCardDao.findNotDeletedByCardNumber("HR-1002")).containsExactly(renewed);
    }

    @Test
    void validityMustNotEndBeforeItStarts() {
        assertThatThrownBy(() -> fixture.card("1003", "HR-1003", guest, LocalDate.of(2026, 7, 14),
                LocalDate.of(2026, 7, 1))).isInstanceOf(DataIntegrityViolationException.class);
    }
}
