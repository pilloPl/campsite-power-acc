package io.pillopl.reporting;

import io.pillopl.dto.report.ReportLineDTO;
import io.pillopl.persistence.dao.ConsumptionReportDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Multi-series reports for the charts of the console: kWh accumulated per pedestal over months, quarters or years.
 */
@Service
@Transactional(readOnly = true)
public class ReportingService {

    /**
     * Width of one bucket of the accumulated report.
     */
    public enum TimeResolution {
        MONTH,
        QUARTER,
        YEAR
    }

    /**
     * More buckets make no chart; the caller narrows the period or takes a coarser resolution.
     */
    static final int MAX_PERIODS = 120;

    private final ConsumptionReportDao consumptionReportDao;

    public ReportingService(ConsumptionReportDao consumptionReportDao) {
        this.consumptionReportDao = consumptionReportDao;
    }

    /**
     * kWh of each selected pedestal in each month, quarter or year touching [from, to]. Every pedestal has a line in
     * every bucket (0 when it supplied nothing); the series {@link ReportLineDTO#ALL_SERIES} carries the bucket
     * total of kWh and records.
     *
     * @return lines in bucket order, within a bucket the pedestals in the given order and the total last; period
     * "2026_07", "2026_Q3" or "2026"; count = records
     */
    public List<ReportLineDTO> fetchAccumulatedKwhPerPedestal(TimeResolution timeResolution, LocalDateTime from,
                                                              LocalDateTime to, List<String> pedestalCodes,
                                                              Long siteGroupId, Long issuerGroupId) {
        Objects.requireNonNull(timeResolution, "timeResolution");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Start " + from + " is after end " + to);
        }
        Set<String> selectedPedestalCodes = new LinkedHashSet<>(pedestalCodes != null ? pedestalCodes : List.of());
        if (selectedPedestalCodes.isEmpty()) {
            throw new IllegalArgumentException("No pedestal selected for the accumulated kWh");
        }

        List<ReportLineDTO> lines = new ArrayList<>();
        LocalDate periodStart = startOfPeriod(timeResolution, from.toLocalDate());
        int periods = 0;

        while (!periodStart.atStartOfDay().isAfter(to)) {
            if (++periods > MAX_PERIODS) {
                throw new IllegalArgumentException(String.format("More than %d periods of %s between %s and %s",
                        MAX_PERIODS, timeResolution, from, to));
            }
            LocalDate nextPeriodStart = nextPeriod(timeResolution, periodStart);
            String period = periodLabel(timeResolution, periodStart);

            List<Object[]> pedestalSums = consumptionReportDao.sumKwhPerPedestal(periodStart.atStartOfDay(),
                    nextPeriodStart.atStartOfDay(), selectedPedestalCodes, siteGroupId, issuerGroupId);
            Map<String, Object[]> sumPerPedestal = new HashMap<>();
            for (Object[] pedestalSum : pedestalSums) {
                sumPerPedestal.put((String) pedestalSum[0], pedestalSum);
            }

            BigDecimal sumOfKwh = BigDecimal.ZERO;
            long sumOfRecords = 0;
            Map<String, ReportLineDTO> periodLines = new LinkedHashMap<>();
            for (String pedestalCode : selectedPedestalCodes) {
                Object[] pedestalSum = sumPerPedestal.get(pedestalCode);
                if (pedestalSum != null) {
                    BigDecimal kwh = ((BigDecimal) pedestalSum[1]).setScale(CampsiteReportService.KWH_SCALE,
                            RoundingMode.HALF_UP);
                    long records = (Long) pedestalSum[2];
                    periodLines.put(pedestalCode, new ReportLineDTO(period, pedestalCode, kwh, records, null));
                    sumOfKwh = sumOfKwh.add(kwh);
                    sumOfRecords = sumOfRecords + records;
                } else {
                    periodLines.put(pedestalCode, new ReportLineDTO(period, pedestalCode,
                            BigDecimal.ZERO.setScale(CampsiteReportService.KWH_SCALE), 0, null));
                }
            }
            lines.addAll(periodLines.values());
            lines.add(new ReportLineDTO(period, ReportLineDTO.ALL_SERIES, sumOfKwh, sumOfRecords, null));

            periodStart = nextPeriodStart;
        }
        return lines;
    }

    static LocalDate startOfPeriod(TimeResolution timeResolution, LocalDate day) {
        switch (timeResolution) {
            case MONTH:
                return day.withDayOfMonth(1);
            case QUARTER:
                return LocalDate.of(day.getYear(), ((day.getMonthValue() - 1) / 3) * 3 + 1, 1);
            case YEAR:
                return day.withDayOfYear(1);
            default:
                throw new IllegalArgumentException("TimeResolution " + timeResolution + " not implemented");
        }
    }

    static LocalDate nextPeriod(TimeResolution timeResolution, LocalDate periodStart) {
        switch (timeResolution) {
            case MONTH:
                return periodStart.plusMonths(1);
            case QUARTER:
                return periodStart.plusMonths(3);
            case YEAR:
                return periodStart.plusYears(1);
            default:
                throw new IllegalArgumentException("TimeResolution " + timeResolution + " not implemented");
        }
    }

    static String periodLabel(TimeResolution timeResolution, LocalDate periodStart) {
        switch (timeResolution) {
            case MONTH:
                return String.format("%d_%02d", periodStart.getYear(), periodStart.getMonthValue());
            case QUARTER:
                return String.format("%d_Q%d", periodStart.getYear(), (periodStart.getMonthValue() - 1) / 3 + 1);
            case YEAR:
                return String.valueOf(periodStart.getYear());
            default:
                throw new IllegalArgumentException("TimeResolution " + timeResolution + " not implemented");
        }
    }
}
