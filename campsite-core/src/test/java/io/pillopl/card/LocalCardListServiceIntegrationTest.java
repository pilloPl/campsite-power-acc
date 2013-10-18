package io.pillopl.card;

import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.group.attribute.LocalListAttribute;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.persistence.dao.HouseCardDao;
import io.pillopl.persistence.dao.LocalListAttributeDao;
import io.pillopl.persistence.dao.LocalListVersionDao;
import io.pillopl.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class LocalCardListServiceIntegrationTest extends IntegrationTest {

    @Autowired LocalCardListService localCardListService;
    @Autowired LocalListAttributeDao localListAttributeDao;
    @Autowired LocalListVersionDao localListVersionDao;
    @Autowired HouseCardDao houseCardDao;
    @Autowired GroupService groupService;
    @Autowired GroupManager groupManager;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired CardFixture cards;

    Group bay;
    Pedestal pedestal;

    @BeforeEach
    void setUp() {
        bay = groupManager.createChildGroup(groupService.fetchSiteGroup("HR SITES"), "BAY CAMPSITE", "Bay");
        Group club = groupManager.createChildGroup(groupService.fetchIssuerGroup("HR ISSUERS"), "CARAVAN CLUB",
                "Club");
        groupManager.addAcceptanceAgreement(club, bay);
        pedestal = infrastructure.pedestal("HR-BAY-P001", bay, 2);
        LocalListAttribute attribute = new LocalListAttribute(true, true, true);
        attribute.setGroup(bay);
        localListAttributeDao.saveAndFlush(attribute);
        HouseCard houseCard = new HouseCard(HouseCardType.STAFF, "9501", "S1");
        houseCard.setGroup(bay);
        houseCardDao.saveAndFlush(houseCard);
        cards.card("9502", "N9502", cards.guest("Kovac", club), LocalDate.now(), LocalDate.now().plusDays(3));
    }

    @Test
    void listOfThePedestalHoldsItsHouseCardsAndTheAcceptedCards() {
        LocalCardList list = localCardListService.buildLocalCardList("HR-BAY-P001");

        assertThat(list.entries()).extracting(LocalCardListEntry::rfid).containsExactly("9501", "9502");
        assertThat(list.capacity()).isEqualTo(250);
    }

    @Test
    void versionGrowsWithEveryList() {
        assertThat(localCardListService.getNextLocalListVersion(pedestal)).isEqualTo(1);
        assertThat(localCardListService.getNextLocalListVersion(pedestal)).isEqualTo(2);
        assertThat(localListVersionDao.findByPedestal(pedestal).getVersion()).isEqualTo(2);
    }

    @Test
    void withoutPedestalLinkTheListIsNotSent() {
        assertThatThrownBy(() -> localCardListService.sendFull("HR-BAY-P001", false))
                .isInstanceOf(LocalCardListException.class)
                .hasMessageContaining(NoLinkLocalCardListSender.NO_LINK);
    }
}
