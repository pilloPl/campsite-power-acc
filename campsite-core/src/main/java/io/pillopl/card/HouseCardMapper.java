package io.pillopl.card;

import io.pillopl.dto.card.HouseCardDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * House cards to DTOs.
 */
public final class HouseCardMapper {

    private HouseCardMapper() {
        throw new AssertionError();
    }

    public static HouseCardDTO toDTO(HouseCard houseCard) {
        return new HouseCardDTO(houseCard.getId(),
                houseCard.getHouseCardType() != null ? houseCard.getHouseCardType().name() : null,
                houseCard.getRfid(), houseCard.getCardNumber(), houseCard.isActive(), houseCard.getGroupId(),
                houseCard.getGroup() != null ? houseCard.getGroup().getDisplayName() : null,
                houseCard.getCreationDate(), houseCard.getLastUsed());
    }

    public static List<HouseCardDTO> toDTOs(List<HouseCard> houseCards) {
        List<HouseCardDTO> result = new ArrayList<>(houseCards.size());
        for (HouseCard houseCard : houseCards) {
            result.add(toDTO(houseCard));
        }
        return result;
    }
}
