package io.pillopl.card;

import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.persistence.dao.HouseCardDao;
import io.pillopl.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class HouseCardIntegrationTest extends IntegrationTest {

    @Autowired HouseCardDao houseCardDao;
    @Autowired GroupService groupService;
    @Autowired GroupManager groupManager;

    Group bay;

    @BeforeEach
    void setUp() {
        bay = groupManager.createChildGroup(groupService.fetchSiteGroup("HR SITES"), "BAY CAMPSITE", "Bay");
    }

    @Test
    void aCampsiteKeepsManyHouseCards() {
        HouseCard staff = save(new HouseCard(HouseCardType.STAFF, "5001", "BAY-S1"));
        HouseCard rental = save(new HouseCard(HouseCardType.RENTAL, "5002", "BAY-R1"));

        assertThat(houseCardDao.findActiveByGroupIds(List.of(bay.getId()))).containsExactlyInAnyOrder(staff, rental);
        assertThat(houseCardDao.findActiveByRfidAndNumber("5002", "BAY-R1")).containsExactly(rental);
        assertThat(houseCardDao.findActiveByRfidAndNumber("5002", null)).containsExactly(rental);
        assertThat(rental.isRental()).isTrue();
    }

    @Test
    void deactivatedHouseCardsAreListedButNotAccepted() {
        HouseCard card = new HouseCard(HouseCardType.STAFF, "5003", "BAY-S2");
        card.setActive(false);
        save(card);

        assertThat(houseCardDao.findActiveByGroupIds(List.of(bay.getId()))).isEmpty();
        assertThat(houseCardDao.findForGroupList(List.of(bay.getId()))).containsExactly(card);
    }

    @Test
    void deletedHouseCardsDisappear() {
        HouseCard card = save(new HouseCard(HouseCardType.STAFF, "5004", "BAY-S3"));
        card.markDeleted(LocalDateTime.now());
        houseCardDao.saveAndFlush(card);

        assertThat(houseCardDao.findForGroupList(List.of(bay.getId()))).isEmpty();
        assertThat(houseCardDao.findActiveByRfidAndNumber("5004", null)).isEmpty();
        assertThat(houseCardDao.findById(card.getId()).orElseThrow().getDeletedDate()).isNotNull();
    }

    private HouseCard save(HouseCard card) {
        card.setGroup(bay);
        return houseCardDao.saveAndFlush(card);
    }
}
