package io.pillopl.group;

import io.pillopl.card.CardFixture;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.persistence.dao.UserinfoDao;
import io.pillopl.test.IntegrationTest;
import io.pillopl.user.Userinfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class GroupValidityIntegrationTest extends IntegrationTest {

    @Autowired GroupService groupService;
    @Autowired UserinfoDao userinfoDao;
    @Autowired InfrastructureFixture fixture;
    @Autowired CardFixture cards;

    @Test
    void usersInStaffGroupsAndPedestalsInSiteGroupsAreValid() {
        user(groupService.fetchStaffGroup("HR RECEPTION"));
        user(null);
        fixture.pedestal("HR-BAY-P001", groupService.fetchSiteGroup("HR SITES"), 1);

        assertThat(groupService.checkAllGroupsForValidity()).isTrue();
    }

    @Test
    void userAttachedToASiteGroupMakesTheTreesInvalid() {
        user(groupService.fetchSiteGroup("HR SITES"));

        assertThat(groupService.checkAllGroupsForValidity()).isFalse();
    }

    @Test
    void pedestalWithoutGroupMakesTheTreesInvalid() {
        fixture.pedestal("HR-BAY-P001", null, 1);

        assertThat(groupService.checkAllGroupsForValidity()).isFalse();
    }

    @Test
    void pedestalInAnIssuerGroupMakesTheTreesInvalid() {
        fixture.pedestal("HR-BAY-P001", groupService.fetchNetworkCardIssuerGroup(), 1);

        assertThat(groupService.checkAllGroupsForValidity()).isFalse();
    }

    @Test
    void guestsInIssuerGroupsAreValid() {
        cards.guest("Horvat", groupService.fetchNetworkCardIssuerGroup());

        assertThat(groupService.checkAllGroupsForValidity()).isTrue();
    }

    @Test
    void guestInASiteGroupMakesTheTreesInvalid() {
        cards.guest("Horvat", groupService.fetchSiteGroup("HR SITES"));

        assertThat(groupService.checkAllGroupsForValidity()).isFalse();
    }

    private void user(Group group) {
        Userinfo user = new Userinfo("u" + System.nanoTime(), "User", "user@example.org");
        user.setGroup(group);
        userinfoDao.saveAndFlush(user);
    }
}
