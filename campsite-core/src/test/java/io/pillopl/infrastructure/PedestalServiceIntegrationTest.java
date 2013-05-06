package io.pillopl.infrastructure;

import io.pillopl.group.AccessDeniedException;
import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.group.GroupType;
import io.pillopl.persistence.dao.UserinfoDao;
import io.pillopl.test.IntegrationTest;
import io.pillopl.user.Userinfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class PedestalServiceIntegrationTest extends IntegrationTest {

    @Autowired PedestalService pedestalService;
    @Autowired GroupService groupService;
    @Autowired GroupManager groupManager;
    @Autowired UserinfoDao userinfoDao;
    @Autowired InfrastructureFixture fixture;

    Group bay, cove;
    Userinfo bayManager, admin;

    @BeforeEach
    void setUp() {
        Group hrSites = groupService.fetchSiteGroup("HR SITES");
        bay = groupManager.createChildGroup(hrSites, "BAY CAMPSITE", "Bay", GroupType.SITE);
        cove = groupManager.createChildGroup(hrSites, "COVE CAMPSITE", "Cove", GroupType.SITE);
        Group hrStaff = groupService.fetchStaffGroup("HR STAFF");
        admin = user("admin", hrStaff, hrStaff, hrSites);
        bayManager = user("bay", groupService.fetchStaffGroup("HR RECEPTION"), bay);
    }

    @Test
    void managerSeesOnlyThePedestalsOfItsCampsite() {
        Pedestal atBay = fixture.pedestal("HR-BAY-P001", bay, 1);
        Pedestal atCove = fixture.pedestal("HR-COVE-P001", cove, 1);

        assertThat(pedestalService.isManageable(bayManager, atBay)).isTrue();
        assertThat(pedestalService.isManageable(bayManager, atCove)).isFalse();
        assertThat(pedestalService.findByCodeForUser(bayManager, "HR-COVE-P001")).isNull();
        assertThat(pedestalService.findByCodeForUser(admin, "HR-COVE-P001")).isEqualTo(atCove);
        assertThatThrownBy(() -> pedestalService.checkIsManageable(bayManager, atCove))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void pedestalListIsPagedWithTheTotalCount() {
        fixture.pedestal("HR-BAY-P001", bay, 1);
        fixture.pedestal("HR-BAY-P002", bay, 1);
        fixture.pedestal("HR-BAY-P003", bay, 1);

        PedestalPage page = pedestalService.findPedestalsByState(bayManager, null, 0, 2);

        assertThat(page.pedestals()).hasSize(2);
        assertThat(page.total()).isEqualTo(3);
        assertThat(pedestalService.findPedestalsByState(bayManager, null, null, null).total()).isEqualTo(3);
    }

    @Test
    void ownerDeletesItsPedestalWithItsSockets() {
        Pedestal pedestal = fixture.pedestal("HR-BAY-P001", bay, 2);

        pedestalService.deletePedestal(admin, bayManager, "HR-BAY-P001");

        assertThat(pedestal.isDeleted()).isTrue();
        assertThat(pedestal.getSockets()).allMatch(Socket::isDeleted);
    }

    @Test
    void ownerMayNotDeleteAPedestalOfAnotherCampsite() {
        fixture.pedestal("HR-COVE-P001", cove, 1);

        assertThatThrownBy(() -> pedestalService.deletePedestal(admin, bayManager, "HR-COVE-P001"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void pitchHasAtMostOneSocketOnACampsite() {
        Pedestal pedestal = fixture.pedestal("HR-BAY-P001", bay, 2);
        Long first = pedestal.getSockets().get(0).getId();
        Long second = pedestal.getSockets().get(1).getId();

        assertThat(pedestalService.assignPitch(bayManager, first, " a-17 ").getPitchNo()).isEqualTo("A-17");
        assertThatThrownBy(() -> pedestalService.assignPitch(bayManager, second, "A-17"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(pedestalService.assignPitch(bayManager, first, "").getPitchNo()).isNull();
    }

    @Test
    void freeSocketIsSetOnlyByAManager() {
        Pedestal atCove = fixture.pedestal("HR-COVE-P001", cove, 1);
        Long socketId = atCove.getSockets().get(0).getId();

        assertThatThrownBy(() -> pedestalService.setFreeSocket(bayManager, socketId, true))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(pedestalService.setFreeSocket(admin, socketId, true).isFreeSocket()).isTrue();
    }

    @Test
    void firstMessageCreatesTheLinkStateAndAMessageAfterTimeoutReportsThePedestalBack() {
        Pedestal pedestal = fixture.pedestal("HR-BAY-P001", bay, 1);
        LocalDateTime noon = LocalDateTime.of(2026, 7, 1, 12, 0);

        assertThat(pedestalService.registerIncoming(pedestal, noon)).isFalse();
        PedestalState state = pedestalService.findState(pedestal.getId());
        state.checkTimeout(noon.plusHours(1), 15);

        assertThat(pedestalService.registerIncoming(pedestal, noon.plusHours(2))).isTrue();
        assertThat(state.isTimeout()).isFalse();
        assertThat(state.getApproxLastIncoming()).isEqualTo(noon.plusHours(2));
    }

    @Test
    void linkStateIsNotRewrittenForEveryMessage() {
        Pedestal pedestal = fixture.pedestal("HR-BAY-P001", bay, 1);
        LocalDateTime noon = LocalDateTime.of(2026, 7, 1, 12, 0);
        pedestalService.registerIncoming(pedestal, noon);

        pedestalService.registerIncoming(pedestal, noon.plusSeconds(30));
        assertThat(pedestalService.findState(pedestal.getId()).getApproxLastIncoming()).isEqualTo(noon);

        pedestalService.registerIncoming(pedestal, noon.plusSeconds(90));
        assertThat(pedestalService.findState(pedestal.getId()).getApproxLastIncoming()).isEqualTo(noon.plusSeconds(90));
    }

    private Userinfo user(String username, Group ownGroup, Group... scope) {
        Userinfo user = new Userinfo(username, username, username + "@example.org");
        user.setGroup(ownGroup);
        for (Group g : scope) {
            user.getGroups().add(g);
        }
        return userinfoDao.saveAndFlush(user);
    }
}
