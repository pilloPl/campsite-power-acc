package io.pillopl.infrastructure;

import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.group.GroupType;
import io.pillopl.persistence.dao.PedestalDao;
import io.pillopl.persistence.dao.PedestalStateDao;
import io.pillopl.test.IntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tree: HR SITES / BAY CAMPSITE / BAY BEACH SECTION and HR SITES / COVE CAMPSITE;
 * HR ISSUERS / CARAVAN CLUB / CARAVAN CLUB JUNIOR.
 */
@Transactional
class PedestalDaoIntegrationTest extends IntegrationTest {

    @Autowired PedestalDao pedestalDao;
    @Autowired PedestalStateDao pedestalStateDao;
    @Autowired GroupService groupService;
    @Autowired GroupManager groupManager;
    @Autowired InfrastructureFixture fixture;
    @Autowired EntityManager entityManager;

    Group hrSites, bay, beach, cove, club, junior;

    @BeforeEach
    void tree() {
        hrSites = groupService.fetchSiteGroup("HR SITES");
        bay = groupManager.createChildGroup(hrSites, "BAY CAMPSITE", "Bay", GroupType.SITE);
        beach = groupManager.createChildGroup(bay, "BAY BEACH SECTION", "Beach", GroupType.SITE);
        cove = groupManager.createChildGroup(hrSites, "COVE CAMPSITE", "Cove", GroupType.SITE);
        club = groupManager.createChildGroup(groupService.fetchIssuerGroup("HR ISSUERS"), "CARAVAN CLUB", "Club",
                GroupType.ISSUER);
        junior = groupManager.createChildGroup(club, "CARAVAN CLUB JUNIOR", "Junior", GroupType.ISSUER);
    }

    @Test
    void pedestalIsFoundByCodeOnlyInsideTheUsersSiteSubtree() {
        Pedestal p = fixture.pedestal("HR-BAY-P001", beach, 1);

        assertThat(pedestalDao.findByCodeRestrictedToUserSiteGroup("HR-BAY-P001", bay.getId())).isEqualTo(p);
        assertThat(pedestalDao.findByCodeRestrictedToUserSiteGroup("HR-BAY-P001", beach.getId())).isEqualTo(p);
        assertThat(pedestalDao.findByCodeRestrictedToUserSiteGroup("HR-BAY-P001", cove.getId())).isNull();
    }

    @Test
    void guestSeesPedestalsOfTheAcceptingCampsitesWithinTheUsersScope() {
        Pedestal atBeach = fixture.pedestal("HR-BAY-P001", beach, 1);
        Pedestal atCove = fixture.pedestal("HR-COVE-P001", cove, 1);
        groupManager.addAcceptanceAgreement(club, bay);

        assertThat(pedestalDao.findPedestalsForGuestRestrictedToUserSiteGroup(hrSites.getId(), junior.getId()))
                .containsExactly(atBeach);
        assertThat(pedestalDao.findPedestalsForGuestRestrictedToUserSiteGroup(cove.getId(), junior.getId()))
                .isEmpty();
        assertThat(atCove).isNotNull();
    }

    @Test
    void nearestAgreementListReplacesTheListsAboveIt() {
        fixture.pedestal("HR-BAY-P001", beach, 1);
        Pedestal atCove = fixture.pedestal("HR-COVE-P001", cove, 1);
        groupManager.addAcceptanceAgreement(club, bay);
        groupManager.addAcceptanceAgreement(junior, cove);

        assertThat(pedestalDao.findPedestalsForGuestRestrictedToUserSiteGroup(hrSites.getId(), junior.getId()))
                .containsExactly(atCove);
    }

    @Test
    void sqlAgreesWithTheInMemoryAgreementWalk() {
        Pedestal atBeach = fixture.pedestal("HR-BAY-P001", beach, 1);
        Pedestal atCove = fixture.pedestal("HR-COVE-P001", cove, 1);
        groupManager.addAcceptanceAgreement(club, bay);

        List<Long> accepting = groupService.extractAllAcceptingSiteGroupIds(junior);
        List<Pedestal> expected = pedestalDao.findByGroupIds(accepting);

        assertThat(pedestalDao.findPedestalsForGuestRestrictedToUserSiteGroup(hrSites.getId(), junior.getId()))
                .containsExactlyElementsOf(expected).contains(atBeach).doesNotContain(atCove);
    }

    @Test
    void pedestalListByState() {
        Pedestal working = fixture.pedestal("HR-BAY-P001", beach, 2);
        Pedestal broken = fixture.pedestal("HR-BAY-P002", beach, 2);
        Pedestal silent = fixture.pedestal("HR-BAY-P003", beach, 1);
        Pedestal disabled = fixture.pedestal("HR-BAY-P004", beach, 1);
        LocalDateTime now = LocalDateTime.now();
        for (Pedestal p : List.of(working, broken, disabled)) {
            pedestalStateDao.save(new PedestalState(p, now));
            p.getSockets().forEach(s -> s.updateStatus(SocketStatus.AVAILABLE, now));
        }
        broken.getSockets().get(1).updateStatus(SocketStatus.BREAKER_TRIPPED, now);
        disabled.getSockets().get(0).setMonitoring(false);
        entityManager.flush();

        assertThat(pedestalDao.findPedestalsByState(PedestalListState.WORKING, bay.getId(), null, null))
                .containsExactly(working);
        assertThat(pedestalDao.findPedestalsByState(PedestalListState.ERROR, bay.getId(), null, null))
                .containsExactly(broken, silent);
        assertThat(pedestalDao.findPedestalsByState(PedestalListState.DISABLED, bay.getId(), null, null))
                .containsExactly(disabled);
        assertThat(pedestalDao.countPedestalsByState(PedestalListState.ERROR, bay.getId())).isEqualTo(2);
        assertThat(pedestalDao.countPedestalsByState(null, bay.getId())).isEqualTo(4);
        assertThat(pedestalDao.findPedestalsByState(null, bay.getId(), 1, 3)).containsExactly(disabled);
        assertThat(pedestalDao.countPedestalsByState(null, cove.getId())).isZero();
    }

    @Test
    void pedestalsOfGroupsAreSoftDeletedExceptTheKeptOnes() {
        Pedestal kept = fixture.pedestal("HR-BAY-P001", beach, 1);
        Pedestal gone = fixture.pedestal("HR-BAY-P002", beach, 1);

        int updated = pedestalDao.updatePedestalsDeletedByGroupIds(true, false, List.of(beach.getId()),
                List.of(kept.getId()));
        entityManager.clear();

        assertThat(updated).isEqualTo(1);
        assertThat(pedestalDao.findById(gone.getId()).orElseThrow().isDeleted()).isTrue();
        assertThat(pedestalDao.findById(kept.getId()).orElseThrow().isDeleted()).isFalse();
    }
}
