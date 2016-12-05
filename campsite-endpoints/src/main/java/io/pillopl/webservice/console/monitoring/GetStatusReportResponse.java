package io.pillopl.webservice.console.monitoring;

import io.pillopl.dto.monitoring.StatusReportLineDTO;

import java.io.Serializable;
import java.util.List;

/**
 * Seconds per socket and status in the month.
 */
public record GetStatusReportResponse(List<StatusReportLineDTO> lines) implements Serializable {

    public GetStatusReportResponse {
        lines = lines != null ? List.copyOf(lines) : List.of();
    }
}
