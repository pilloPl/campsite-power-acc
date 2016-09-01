package io.pillopl.dto.guest;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * An order (booking) of a guest: seasonal contract, packages or cards, waiting for the ERP or active.
 */
public record OrderDTO(Long id, String orderNumber, Long guestId, String status, String guestType,
                       List<String> items, LocalDateTime createDate, LocalDateTime activationDate)
        implements Serializable {

    public OrderDTO {
        items = items != null ? List.copyOf(items) : List.of();
    }
}
