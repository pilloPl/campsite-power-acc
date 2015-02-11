package io.pillopl.api;

import io.pillopl.commons.DateRange;
import io.pillopl.dto.report.ReportLineDTO;
import io.pillopl.value.Username;

import java.util.List;

/**
 * Management reports of the caller's campsites (SITE scope) and guests (ISSUER scope), as lines for the charts of
 * the console.
 */
public interface ReportApi {

    String AVERAGE_PER_HOUR_OF_DAY = "AVERAGE_PER_HOUR_OF_DAY";
    String AVERAGE_PER_DAY_OF_WEEK = "AVERAGE_PER_DAY_OF_WEEK";
    String MONTHLY_CONSUMPTION = "MONTHLY_CONSUMPTION";
    String QUARTERLY_CONSUMPTION = "QUARTERLY_CONSUMPTION";
    String CONSUMPTION_PER_STAY_NIGHT = "CONSUMPTION_PER_STAY_NIGHT";
    String ACCUMULATED_KWH_PER_PEDESTAL = "ACCUMULATED_KWH_PER_PEDESTAL";
    String SUPPLY_INCIDENTS_PER_PEDESTAL = "SUPPLY_INCIDENTS_PER_PEDESTAL";

    /**
     * @param reportType      one of the constants of this interface
     * @param period          plug-out (incidents: creation) period, both ends required except for the monthly and
     *                        quarterly consumption, which take year and month instead
     * @param campsiteGroupId a SITE group of the caller's subtree to report on, null for the caller's whole scope
     * @param powerSectionId  only the sockets of this section switchboard, null for all
     * @param year            monthly and quarterly consumption: null for every year
     * @param month           monthly consumption: 1..12, null for every month
     * @param timeResolution  accumulated kWh: MONTH, QUARTER or YEAR
     * @param pedestalCodes   accumulated kWh: the pedestals of the chart
     */
    List<ReportLineDTO> getReport(Username authUsername, String reportType, DateRange period, Long campsiteGroupId,
                                  Long powerSectionId, Integer year, Integer month, String timeResolution,
                                  List<String> pedestalCodes);
}
