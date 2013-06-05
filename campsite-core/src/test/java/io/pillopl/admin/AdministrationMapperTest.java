package io.pillopl.admin;

import io.pillopl.config.SystemProperty;
import io.pillopl.config.SystemPropertyKey;
import io.pillopl.dto.admin.SystemPropertyDTO;
import io.pillopl.dto.user.UserroleDTO;
import io.pillopl.user.Userright;
import io.pillopl.user.Userrole;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdministrationMapperTest {

    @Test
    void everyKeyIsListedAndOnlyStoredKeysHaveAValue() {
        List<SystemPropertyDTO> dtos = AdministrationMapper.toSystemPropertyDTOs(List.of(
                new SystemProperty(SystemPropertyKey.StaleSessionTimeoutInHours, "24", "stale after")));

        assertThat(dtos).hasSize(SystemPropertyKey.values().length);
        assertThat(dtos).filteredOn(d -> d.key().equals("StaleSessionTimeoutInHours"))
                .singleElement().extracting(SystemPropertyDTO::value).isEqualTo("24");
        assertThat(dtos).filteredOn(d -> d.key().equals("DeveloperEmail"))
                .singleElement().extracting(SystemPropertyDTO::value).isNull();
    }

    @Test
    void roleGoesOutWithItsGroupTypesAndSortedRights() {
        Userrole role = new Userrole("RECEPTION");
        role.setGroupTypes("STAFF, SITE,ISSUER");
        role.getRights().add(new Userright("STAY_CHECKOUT"));
        role.getRights().add(new Userright("CARD_VIEW"));

        UserroleDTO dto = AdministrationMapper.toDTO(role);

        assertThat(dto.groupTypes()).containsExactly("STAFF", "SITE", "ISSUER");
        assertThat(dto.rights()).containsExactly("CARD_VIEW", "STAY_CHECKOUT");
        assertThat(AdministrationMapper.toDTO(new Userrole("X")).groupTypes()).isEmpty();
    }
}
