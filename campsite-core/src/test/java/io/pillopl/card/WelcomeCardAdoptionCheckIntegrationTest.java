package io.pillopl.card;

import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.group.attribute.WelcomeCardAdoption;
import io.pillopl.group.attribute.WelcomeCardAdoptionService;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.persistence.dao.CampingCardDao;
import io.pillopl.persistence.dao.UserinfoDao;
import io.pillopl.persistence.dao.WelcomeCardAdoptionDao;
import io.pillopl.test.IntegrationTest;
import io.pillopl.user.Userinfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CAMP-84: a welcome card from the market's pool becomes a house card of the affiliate campsite where it is used.
 */
@Transactional
class WelcomeCardAdoptionCheckIntegrationTest extends IntegrationTest {

    @Autowired WelcomeCardAdoptionService welcomeCardAdoptionService;
    @Autowired WelcomeCardAdoptionDao welcomeCardAdoptionDao;
    @Autowired HouseCardService houseCardService;
    @Autowired CampingCardDao campingCardDao;
    @Autowired GroupService groupService;
    @Autowired GroupManager groupManager;
    @Autowired UserinfoDao userinfoDao;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired CardFixture cards;

    Group affiliate;
    CampingCard welcomeCard;

    @BeforeEach
    void setUp() {
        affiliate = groupManager.createChildGroup(groupService.fetchSiteGroup("HOUSE CARDS ONLY HR"),
                "AFFILIATE marko", "Marko's campsite");
        Group section = groupManager.createChildGroup(affiliate, "MARKO MEADOW", "Meadow");
        Userinfo owner = new Userinfo("marko", "Marko", "marko@example.org");
        owner.setGroup(groupService.fetchStaffGroup("HR SELF SERVICE"));
        owner.getGroups().add(affiliate);
        userinfoDao.saveAndFlush(owner);
        infrastructure.pedestal("HR-MARKO-P001", section, 2);
        welcomeCard = cards.card("8001", cards.guest("Welcome kit 17",
                groupService.fetchIssuerGroup("HR WELCOME_CARD_POOL")));
    }

    @Test
    void welcomeCardIsAdoptedAsHouseCardAndLeavesThePool() {
        allowAdoption(true);

        assertThat(welcomeCardAdoptionService.checkAndAdopt("HR-MARKO-P001", "8001")).isTrue();

        HouseCard houseCard = houseCardService.getHouseCardForPedestal("HR-MARKO-P001", "8001");
        assertThat(houseCard).isNotNull();
        assertThat(houseCard.getHouseCardType()).isEqualTo(HouseCardType.STAFF);
        assertThat(houseCard.getCardNumber()).isEqualTo(welcomeCard.getCardNumber());
        assertThat(houseCard.getGroupId()).isEqualTo(affiliate.getId());
        assertThat(campingCardDao.findByCardRfid("8001")).isNull();
    }

    @Test
    void campsiteMustAllowTheAdoption() {
        assertThat(welcomeCardAdoptionService.checkAndAdopt("HR-MARKO-P001", "8001")).isFalse();

        allowAdoption(false);

        assertThat(welcomeCardAdoptionService.checkAndAdopt("HR-MARKO-P001", "8001")).isFalse();
        assertThat(campingCardDao.findByCardRfid("8001")).isNotNull();
    }

    @Test
    void campsiteWithNetworkAccessDoesNotAdopt() {
        allowAdoption(true);
        affiliate.setParent(groupService.fetchSiteGroup("NETWORK ACCESS HR"));

        assertThat(welcomeCardAdoptionService.checkAndAdopt("HR-MARKO-P001", "8001")).isFalse();
    }

    @Test
    void onlyCardsOfTheWelcomeCardPoolAreAdopted() {
        allowAdoption(true);
        cards.card("8002", cards.guest("Horvat", groupService.fetchNetworkCardIssuerGroup()));

        assertThat(welcomeCardAdoptionService.checkAndAdopt("HR-MARKO-P001", "8002")).isFalse();
        assertThat(welcomeCardAdoptionService.checkAndAdopt("HR-MARKO-P001", "8999")).isFalse();
        assertThat(welcomeCardAdoptionService.checkAndAdopt("HR-NONE-P001", "8001")).isFalse();
    }

    private void allowAdoption(boolean allowed) {
        WelcomeCardAdoption adoption = new WelcomeCardAdoption(allowed);
        adoption.setGroup(affiliate);
        welcomeCardAdoptionDao.saveAndFlush(adoption);
    }
}
