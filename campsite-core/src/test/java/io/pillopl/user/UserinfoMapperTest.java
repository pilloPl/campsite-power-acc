package io.pillopl.user;

import io.pillopl.dto.user.UserinfoDTO;
import io.pillopl.group.Group;
import io.pillopl.group.GroupType;
import io.pillopl.infrastructure.Commissioning;
import io.pillopl.infrastructure.CommissioningMapper;
import io.pillopl.infrastructure.Location;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserinfoMapperTest {

    @Test
    void userGoesOutWithGroupsAndRoleNamesButWithoutPassword() {
        Userinfo user = new Userinfo("marko", "Marko", "marko@example.org");
        user.setPasswordHash("secret");
        Group selfService = new Group(GroupType.STAFF, "HR SELF SERVICE", "Self service");
        Group affiliate = new Group(GroupType.SITE, "AFFILIATE marko", "Marko");
        user.setGroup(selfService);
        user.getGroups().add(affiliate);
        user.getRoles().add(new Userrole(UserroleName.AFFILIATE_OWNER));

        UserinfoDTO dto = UserinfoMapper.toDTO(user);

        assertThat(dto.group().name()).isEqualTo("HR SELF SERVICE");
        assertThat(dto.scopeGroups()).extracting("name").containsExactly("AFFILIATE marko");
        assertThat(dto.roles()).containsExactly("AFFILIATE_OWNER");
        assertThat(dto.status()).isEqualTo("INVITED");
        assertThat(dto.toString()).doesNotContain("secret");
    }

    @Test
    void commissioningGoesOutWithOwnerAndAccessMode() {
        Commissioning commissioning = new Commissioning();
        commissioning.setAffiliateOwner(new Userinfo("marko", "Marko", "marko@example.org"));
        commissioning.setLocation(new Location("Rovinj", "52210", "Obala", "12"));

        assertThat(CommissioningMapper.toDTO(commissioning).ownerUsername()).isEqualTo("marko");
        assertThat(CommissioningMapper.toDTO(commissioning).accessMode()).isEqualTo("HOUSE_CARDS_ONLY");
        assertThat(CommissioningMapper.toDTO(commissioning).location().city()).isEqualTo("Rovinj");
    }
}
