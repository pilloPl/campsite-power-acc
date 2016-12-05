package io.pillopl.dto.monitoring;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A status of a socket as the pedestal reported it (or monitoring corrected it), with the meter, the measured current
 * and the error flags read with the masks of the manufacturer (the error history of the socket).
 */
public record SocketStatusLogDTO(Long id, Long socketId, String socketCode, String socketName, String pedestalCode,
                                 String status, BigDecimal meterKwh, BigDecimal currentA, long errorFlags,
                                 List<String> errors, LocalDateTime createDate, LocalDateTime remoteDate)
        implements Serializable {

    public SocketStatusLogDTO {
        errors = errors != null ? List.copyOf(errors) : List.of();
    }
}
