package io.pillopl.card;

import io.pillopl.group.AccessDeniedException;
import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.group.attribute.LocalListAttribute;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.persistence.dao.LocalListAttributeDao;
import io.pillopl.persistence.dao.UserinfoDao;
import io.pillopl.test.IntegrationTest;
import io.pillopl.user.Userinfo;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class HouseCardServiceIntegrationTest extends IntegrationTest {

    @Autowired HouseCardService houseCardService;
    @Autowired GroupService groupService;
    @Autowired GroupManager groupManager;
    @Autowired UserinfoDao userinfoDao;
    @Autowired LocalListAttributeDao localListAttributeDao;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired EntityManager entityManager;

    Group bay, bayBeach, lake, ownerStaff;
    Userinfo owner, marketAdmin, otherOwner;
    Pedestal atBayBeach, atLake;

    @BeforeEach
    void setUp() {
        Group hrSites = groupService.fetchSiteGroup("HR SITES");
        bay = groupManager.createChildGroup(groupService.fetchSiteGroup("NETWORK ACCESS HR"), "AFFILIATE marko",
                "Marko's campsite");
        bayBeach = groupManager.createChildGroup(bay, "MARKO BEACH SECTION", "Beach");
        lake = groupManager.createChildGroup(hrSites, "LAKE CAMPSITE", "Lake");
        Group hrStaff = groupService.fetchStaffGroup("HR STAFF");
        ownerStaff = groupManager.createChildGroup(groupService.fetchStaffGroup("HR SELF SERVICE"), "AFFILIATE marko",
                "Marko");
        owner = user("marko", ownerStaff, bay);
        otherOwner = user("ivana", groupService.fetchStaffGroup("HR SELF SERVICE"), lake);
        marketAdmin = user("admin", hrStaff, hrStaff, hrSites);
        atBayBeach = infrastructure.pedestal("HR-MARKO-P001", bayBeach, 2);
        atLake = infrastructure.pedestal("HR-LAKE-P001", lake, 2);
    }

    @Test
    void houseCardOfTheCampsiteIsAcceptedAtThePedestalsOfItsSubtree() {
        HouseCard card = houseCardService.saveHouseCard("marko", "marko", null, HouseCardType.STAFF, "6001",
                "M-S1", true);

        assertThat(houseCardService.getHouseCardForPedestal("HR-MARKO-P001", "6001")).isEqualTo(card);
        assertThat(houseCardService.getHouseCardsForPedestal("HR-MARKO-P001")).containsExactly(card);
        assertThat(houseCardService.getHouseCardForPedestal("HR-LAKE-P001", "6001")).isNull();
        assertThat(houseCardService.getHouseCardsForPedestal("UNKNOWN")).isEmpty();
    }

    @Test
    void deactivatedOrDeletedHouseCardIsNotAccepted() {
        HouseCard first = houseCardService.saveHouseCard("marko", "marko", null, HouseCardType.RENTAL, "6002",
                "M-R1", true);
        HouseCard second = houseCardService.saveHouseCard("marko", "marko", null, HouseCardType.RENTAL, "6003",
                "M-R2", true);

        assertThat(houseCardService.deactivateHouseCard("marko", "marko", first.getId())).isTrue();
        assertThat(houseCardService.deleteHouseCard("marko", "marko", second.getId())).isTrue();

        assertThat(houseCardService.getHouseCardForPedestal("HR-MARKO-P001", "6002")).isNull();
        assertThat(houseCardService.getHouseCardForPedestal("HR-MARKO-P001", "6003")).isNull();
        assertThat(houseCardService.getHouseCards("marko", "marko")).containsExactly(first);
        assertThat(houseCardService.activateHouseCard("marko", "marko", first.getId())).isTrue();
        assertThat(houseCardService.getHouseCardForPedestal("HR-MARKO-P001", "6002")).isEqualTo(first);
        assertThat(houseCardService.deleteHouseCard("marko", "marko", second.getId())).isFalse();
        assertThat(houseCardService.activateHouseCard("marko", "marko", 999999L)).isFalse();
    }

    @Test
    void staffManagingTheOwnerAdministersItsHouseCards() {
        HouseCard card = houseCardService.saveHouseCard("admin", "marko", null, HouseCardType.STAFF, "6004",
                "M-S2", true);

        assertThat(card.getGroupId()).isEqualTo(bay.getId());
        assertThat(houseCardService.checkUserPrivileges("admin", "marko")).isEqualTo(owner);
        assertThat(houseCardService.getHouseCards("admin", "marko")).containsExactly(card);
    }

    @Test
    void anotherOwnerMayNotAdministerTheHouseCards() {
        assertThatThrownBy(() -> houseCardService.checkUserPrivileges("ivana", "marko"))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> houseCardService.checkUserPrivileges("nobody", "marko"))
                .isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> houseCardService.checkUserPrivileges("admin", "nobody"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void houseCardOfAnotherCampsiteCannotBeChangedThroughTheOwner() {
        HouseCard lakeCard = houseCardService.saveHouseCard("ivana", "ivana", null, HouseCardType.STAFF, "6005",
                "L-S1", true);

        assertThatThrownBy(() -> houseCardService.deactivateHouseCard("marko", "marko", lakeCard.getId()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> houseCardService.saveHouseCard("marko", "marko", lakeCard.getId(),
                HouseCardType.RENTAL, "6005", "L-S1", true)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void houseCardNeedsTypeAndUid() {
        assertThatThrownBy(() -> houseCardService.saveHouseCard("marko", "marko", null, null, "6006", null, true))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("House card type is missing");
        assertThatThrownBy(() -> houseCardService.saveHouseCard("marko", "marko", null, HouseCardType.STAFF, " ",
                null, true)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Card UID of the house card is missing");
    }

    @Test
    void localListsOfTheSubtreeTakeTheHouseCardWhenTheAttributeIncludesHouseCards() {
        HouseCard card = houseCardService.saveHouseCard("marko", "marko", null, HouseCardType.STAFF, "6007",
                "M-S3", true);
        assertThat(houseCardService.findPedestalsForHouseCard(card)).isEmpty();

        LocalListAttribute attribute = new LocalListAttribute(true, true, true);
        attribute.setGroup(bay);
        localListAttributeDao.saveAndFlush(attribute);

        assertThat(houseCardService.findPedestalsForHouseCard(card)).containsExactly(atBayBeach);
        assertThat(houseCardService.findPedestalsForHouseCard(null)).isEmpty();
    }

    @Test
    void useOfAHouseCardIsRemembered() {
        HouseCard card = houseCardService.saveHouseCard("marko", "marko", null, HouseCardType.STAFF, "6008",
                "M-S4", true);

        houseCardService.recordUse(card);
        entityManager.flush();
        entityManager.clear();

        assertThat(houseCardService.getHouseCard(card.getId()).getLastUsed()).isNotNull();
    }

    private Userinfo user(String username, Group ownGroup, Group... scope) {
        Userinfo user = new Userinfo(username, username, username + "@example.org");
        user.setGroup(ownGroup);
        for (Group group : scope) {
            user.getGroups().add(group);
        }
        return userinfoDao.saveAndFlush(user);
    }
}
