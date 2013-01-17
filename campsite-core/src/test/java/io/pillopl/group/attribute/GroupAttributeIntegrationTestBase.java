package io.pillopl.group.attribute;

import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Small tree for attribute tests: HR SITES / BAY CAMPSITE / BAY BEACH SECTION and
 * HR ISSUERS / CARAVAN CLUB / CARAVAN CLUB JUNIOR. Rolled back after each test.
 */
@Transactional
abstract class GroupAttributeIntegrationTestBase extends IntegrationTest {

    @Autowired
    GroupService groupService;

    @Autowired
    GroupManager groupManager;

    Group hrSites, campsite, section;

    Group club, clubJunior;

    @BeforeEach
    void tree() {
        hrSites = groupService.fetchSiteGroup("HR SITES");
        campsite = groupManager.createChildGroup(hrSites, "BAY CAMPSITE", "Bay campsite");
        section = groupManager.createChildGroup(campsite, "BAY BEACH SECTION", "Bay beach");
        club = groupManager.createChildGroup(groupService.fetchIssuerGroup("HR ISSUERS"), "CARAVAN CLUB", "Club");
        clubJunior = groupManager.createChildGroup(club, "CARAVAN CLUB JUNIOR", "Junior");
    }

    static <A extends GroupAttribute> A on(Group group, A attribute) {
        attribute.setGroup(group);
        return attribute;
    }
}
