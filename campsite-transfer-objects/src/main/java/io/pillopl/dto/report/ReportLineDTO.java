package io.pillopl.dto.report;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * One value of a report, ready for a chart: the bucket on the x axis ({@code period}: hour "07", day "MONDAY",
 * month "2026_07", quarter "2026_Q3", year "2026", campsite code, incident type), the series it belongs to (a
 * pedestal code, "ALL" for the total of a bucket, null for single-series reports) and its values.
 *
 * @param kwh   energy of the bucket (sum or average, see the report), null when the report counts only
 * @param count records, sockets, nights or incidents behind the value, see the report
 * @param hours supply hours of the bucket, null when the report has none
 */
public record ReportLineDTO(String period, String series, BigDecimal kwh, long count, BigDecimal hours)
        implements Serializable {

    public static final String ALL_SERIES = "ALL";
}
