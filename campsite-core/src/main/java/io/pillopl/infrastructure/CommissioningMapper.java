package io.pillopl.infrastructure;

import io.pillopl.dto.infrastructure.CommissioningDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * Commissioning entity to transfer object.
 */
public final class CommissioningMapper {

    private CommissioningMapper() {
        throw new AssertionError();
    }

    public static CommissioningDTO toDTO(Commissioning commissioning) {
        if (commissioning == null) {
            return null;
        }
        return new CommissioningDTO(commissioning.getId(),
                commissioning.getAffiliateOwner() != null ? commissioning.getAffiliateOwner().getUsername() : null,
                commissioning.getAffiliateOwner() != null ? commissioning.getAffiliateOwner().getName() : null,
                PedestalMapper.toDTO(commissioning.getLocation()), commissioning.getComments(),
                commissioning.isListedInApp(),
                commissioning.getAccessMode() != null ? commissioning.getAccessMode().name() : null,
                commissioning.getToken(), commissioning.getCreateDate());
    }

    public static List<CommissioningDTO> toDTOs(List<Commissioning> commissionings) {
        List<CommissioningDTO> result = new ArrayList<>(commissionings.size());
        for (Commissioning commissioning : commissionings) {
            result.add(toDTO(commissioning));
        }
        return result;
    }
}
