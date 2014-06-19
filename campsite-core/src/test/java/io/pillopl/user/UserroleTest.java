package io.pillopl.user;

import io.pillopl.group.GroupType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserroleTest {

    @Test
    void roleWithoutConfiguredGroupTypesAllowsEveryTree() {
        Userrole reception = new Userrole(UserroleName.RECEPTION);

        assertThat(reception.allowsGroupType(GroupType.STAFF)).isTrue();
        reception.setGroupTypes("  ");
        assertThat(reception.allowsGroupType(GroupType.ISSUER)).isTrue();
    }

    @Test
    void configuredGroupTypesAreReadWithBlanksAroundTheCommas() {
        Userrole owner = new Userrole(UserroleName.AFFILIATE_OWNER);
        owner.setGroupTypes("STAFF , SITE");

        assertThat(owner.allowsGroupType(GroupType.STAFF)).isTrue();
        assertThat(owner.allowsGroupType(GroupType.SITE)).isTrue();
        assertThat(owner.allowsGroupType(GroupType.ISSUER)).isFalse();
    }

    @Test
    void rolesAndRightsAreComparedByName() {
        Userrole manager = new Userrole(UserroleName.CAMPSITE_MANAGER);
        manager.getRights().add(new Userright("PEDESTAL_COMMISSION"));

        assertThat(manager.hasRight("PEDESTAL_COMMISSION")).isTrue();
        assertThat(manager.hasRight("USER_ADMIN")).isFalse();
        assertThat(manager).isEqualTo(new Userrole("CAMPSITE_MANAGER")).hasSameHashCodeAs(new Userrole("CAMPSITE_MANAGER"));
        assertThat(manager.is(UserroleName.CAMPSITE_MANAGER)).isTrue();
        assertThat(manager.is(UserroleName.RECEPTION)).isFalse();
    }
}
