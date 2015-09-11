package io.pillopl.webservice.console.report;

import io.pillopl.dto.report.ReportLineDTO;

import java.io.Serializable;
import java.util.List;

/**
 * The lines of the report in chart order.
 */
public record GetReportResponse(String reportType, List<ReportLineDTO> lines) implements Serializable {
}
