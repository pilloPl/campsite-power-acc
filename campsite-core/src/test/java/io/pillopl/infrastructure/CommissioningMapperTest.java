package io.pillopl.infrastructure;

import io.pillopl.dto.infrastructure.CommissioningDTO;
import io.pillopl.user.Userinfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommissioningMapperTest {

    @Test
    void commissioningWithoutOwnerGoesOutWithoutOwnerNames() {
        Commissioning commissioning = new Commissioning();
        commissioning.setLocation(new Location("Rovinj", "52210", "Obala", "12"));

        CommissioningDTO dto = CommissioningMapper.toDTO(commissioning);

        assertThat(dto.ownerUsername()).isNull();
        assertThat(dto.ownerName()).isNull();
        assertThat(dto.location().city()).isEqualTo("Rovinj");
    }

    @Test
    void ownerNamesComeFromTheAffiliateOwner() {
        Commissioning commissioning = new Commissioning();
        commissioning.setAffiliateOwner(new Userinfo("marko", "Marko", "marko@example.org"));
        commissioning.setLocation(new Location("Rovinj", "52210", "Obala", "12"));

        List<CommissioningDTO> dtos = CommissioningMapper.toDTOs(List.of(commissioning));

        assertThat(dtos).extracting(CommissioningDTO::ownerName).containsExactly("Marko");
    }
}
