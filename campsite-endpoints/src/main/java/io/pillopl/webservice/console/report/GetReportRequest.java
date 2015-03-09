package io.pillopl.webservice.console.report;

import java.io.Serializable;
import java.util.List;

/**
 * A management report: its type (AVERAGE_PER_HOUR_OF_DAY, AVERAGE_PER_DAY_OF_WEEK, MONTHLY_CONSUMPTION,
 * QUARTERLY_CONSUMPTION, CONSUMPTION_PER_STAY_NIGHT, ACCUMULATED_KWH_PER_PEDESTAL, SUPPLY_INCIDENTS_PER_PEDESTAL),
 * the period (dates "2026-07-01" or date-times), optionally one campsite group and one power section, the year and
 * month of the monthly and quarterly consumption, the time resolution (MONTH, QUARTER, YEAR) and the pedestals of
 * the accumulated kWh.
 */
public record GetReportRequest(String authUsername, String reportType, String from, String to,
                               Long campsiteGroupId, Long powerSectionId, Integer year, Integer month,
                               String timeResolution, List<String> pedestalCodes) implements Serializable {
}
