package io.pillopl.user;

import io.pillopl.group.Group;
import io.pillopl.group.GroupType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserinfoTest {

    @Test
    void scopeGroupIsPickedByTree() {
        Userinfo user = new Userinfo("ana", "Ana", "ana@example.org");
        Group site = new Group(GroupType.SITE, "BAY CAMPSITE");
        Group staff = new Group(GroupType.STAFF, "HR RECEPTION");
        user.getGroups().add(site);
        user.getGroups().add(staff);

        assertThat(user.getGroup(GroupType.SITE)).isSameAs(site);
        assertThat(user.getGroup(GroupType.STAFF)).isSameAs(staff);
        assertThat(user.getGroup(GroupType.ISSUER)).isNull();
    }

    @Test
    void rolesAreComparedByName() {
        Userinfo user = new Userinfo("ana", "Ana", "ana@example.org");
        user.getRoles().add(new Userrole(UserroleName.RECEPTION));

        assertThat(user.containsRole(UserroleName.RECEPTION)).isTrue();
        assertThat(user.removeRole("RECEPTION")).isTrue();
        assertThat(user.containsRole("RECEPTION")).isFalse();
    }

    @Test
    void rightsComeFromDirectRolesAndFromSecurityGroups() {
        Userrole manager = new Userrole(UserroleName.CAMPSITE_MANAGER);
        manager.getRights().add(new Userright("TARIFF_EDIT"));
        SecurityGroup management = new SecurityGroup();
        management.getRoles().add(manager);
        Userinfo user = new Userinfo("ana", "Ana", "ana@example.org");

        assertThat(user.hasRight("TARIFF_EDIT")).isFalse();
        user.getSecurityGroups().add(management);
        assertThat(user.hasRight("TARIFF_EDIT")).isTrue();
    }

    @Test
    void userWithASelfServiceRoleIsASelfServiceUser() {
        Userrole owner = new Userrole(UserroleName.AFFILIATE_OWNER);
        owner.setSelfService(true);
        Userinfo user = new Userinfo("marko", "Marko", "marko@example.org");

        assertThat(user.hasSelfServiceRole()).isFalse();
        user.getRoles().add(owner);
        assertThat(user.hasSelfServiceRole()).isTrue();
    }
}
