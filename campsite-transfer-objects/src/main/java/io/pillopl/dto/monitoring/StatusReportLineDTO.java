package io.pillopl.dto.monitoring;

import java.io.Serializable;

/**
 * Seconds a socket spent in a status during the month of the status report; SUPPLYING_WITHOUT_CONSUMPTION is
 * supplying with a meter that did not move.
 */
public record StatusReportLineDTO(Long socketId, String socketName, Long pedestalId, String pedestalCode,
                                  Long campsiteGroupId, String status, long timeInSeconds) implements Serializable {
}
