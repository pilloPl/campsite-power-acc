package io.pillopl.api;

import io.pillopl.dto.card.HouseCardDTO;
import io.pillopl.value.HouseCardId;
import io.pillopl.value.Username;

import java.util.List;

/**
 * House cards of a campsite, administered by its owner (or the campsite manager) or by staff managing the owner.
 */
public interface HouseCardApi {

    List<HouseCardDTO> findHouseCards(Username authUsername, Username ownerUsername);

    /**
     * @param houseCard without id for a new house card
     */
    HouseCardDTO saveHouseCard(Username authUsername, Username ownerUsername, HouseCardDTO houseCard);

    void deleteHouseCard(Username authUsername, Username ownerUsername, HouseCardId houseCardId);

    void changeHouseCardStatus(Username authUsername, Username ownerUsername, HouseCardId houseCardId,
                               boolean active);
}
