package io.pillopl.dto.guest;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A billing account of a guest; without an ERP number it waits for the ERP to assign one.
 */
public record BillingAccountDTO(Long id, Long guestId, String erpAccountNo, String description,
                                boolean awaitingErpNumber, boolean closed, LocalDateTime createDate)
        implements Serializable {
}
