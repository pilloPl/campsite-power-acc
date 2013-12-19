package io.pillopl.dto.card;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A house card of a campsite.
 */
public record HouseCardDTO(Long id, String houseCardType, String rfid, String cardNumber, boolean active,
                           Long groupId, String groupName, LocalDateTime creationDate, LocalDateTime lastUsed)
        implements Serializable {
}
