package io.pillopl.group;

import io.pillopl.persistence.dao.GroupDao;
import io.pillopl.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class GroupDaoIntegrationTest extends IntegrationTest {

    @Autowired
    GroupDao groupDao;

    @Autowired
    GroupManager groupManager;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private Group site(String name) {
        return groupDao.findByNameAndType(name, GroupType.SITE.name());
    }

    @Test
    void subtreeContainsTheGroupItselfFirstAndThenItsDescendantsLevelByLevel() {
        Group hrSites = site("HR SITES");
        Group bay = groupManager.createChildGroup(site("NETWORK ACCESS HR"), "BAY CAMPSITE", "Bay campsite");
        Group beach = groupManager.createChildGroup(bay, "BAY BEACH SECTION", "Beach");

        assertThat(groupDao.findManageable(hrSites.getId()))
                .containsExactly(hrSites.getId(), 51L, 61L, bay.getId(), beach.getId());
        assertThat(groupDao.findManageableGroups(bay.getId())).containsExactly(bay, beach);
    }

    @Test
    void parentLineGoesFromTheGroupUpToItsRoot() {
        Group bay = groupManager.createChildGroup(site("NETWORK ACCESS HR"), "BAY CAMPSITE", "Bay campsite");
        Group beach = groupManager.createChildGroup(bay, "BAY BEACH SECTION", "Beach");

        assertThat(groupDao.findParents(beach.getId())).containsExactly(beach.getId(), bay.getId(), 51L, 11L);
        assertThat(groupDao.findParentGroups(beach.getId()))
                .extracting(Group::getName)
                .containsExactly("BAY BEACH SECTION", "BAY CAMPSITE", "NETWORK ACCESS HR", "HR SITES");
    }

    @Test
    void groupNameIsUniqueWithinItsType() {
        assertThatThrownBy(() -> groupManager.createChildGroup(site("HR SITES"), "NETWORK ACCESS HR", "Duplicate"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void sameNameMayExistInAnotherType() {
        Group staffRoot = groupDao.findByNameAndType("HR STAFF", GroupType.STAFF.name());

        Group staffGroup = groupManager.createChildGroup(staffRoot, "NETWORK ACCESS HR", "Same name, other tree");

        assertThat(staffGroup.getId()).isGreaterThanOrEqualTo(1000L);
    }

    @Test
    void rootsAreFoundByTypeAndNamesIgnoringCase() {
        assertThat(groupDao.findRoots(GroupType.ISSUER.name()))
                .extracting(Group::getName)
                .containsExactly("HR ISSUERS", "IT ISSUERS", "FR ISSUERS");
        assertThat(groupDao.findByNotCaseSensitiveNameAndType("network card", GroupType.ISSUER.name()).getId())
                .isEqualTo(31L);
    }

    @Test
    void groupTypeIsRestrictedToTheThreeTrees() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO TBL_GROUP (NAME, TYPENAME, DISPLAYNAME) VALUES ('X', 'SALES', 'X')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
