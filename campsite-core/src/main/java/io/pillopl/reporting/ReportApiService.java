package io.pillopl.reporting;

import io.pillopl.api.ReportApi;
import io.pillopl.api.exception.AccessNotAllowedException;
import io.pillopl.api.exception.InvalidInputException;
import io.pillopl.api.exception.ObjectNotFoundException;
import io.pillopl.commons.DateRange;
import io.pillopl.dto.report.ReportLineDTO;
import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.GroupType;
import io.pillopl.monitoring.PedestalStateService;
import io.pillopl.persistence.dao.OperationsViewDao;
import io.pillopl.persistence.dao.PowerSectionDao;
import io.pillopl.reporting.ReportingService.TimeResolution;
import io.pillopl.user.PrincipalResolver;
import io.pillopl.user.Userinfo;
import io.pillopl.user.UserrightName;
import io.pillopl.util.ApiExceptionTranslator;
import io.pillopl.util.ApiValues;
import io.pillopl.value.Username;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Report calls of the console (REPORT_VIEW). The scope is the caller's SITE and ISSUER subtree; a campsite narrows it
 * to one SITE group of the caller's subtree (the issuer side is then left out).
 */
@Service
@Transactional(readOnly = true)
public class ReportApiService implements ReportApi {

    private static final BigDecimal SECONDS_PER_HOUR = BigDecimal.valueOf(3600);

    private static final int HOURS_SCALE = 2;

    private final CampsiteReportService campsiteReportService;

    private final ReportingService reportingService;

    private final GroupService groupService;

    private final PowerSectionDao powerSectionDao;

    private final PrincipalResolver principalResolver;

    private final OperationsViewDao operationsViewDao;

    private final PedestalStateService pedestalStateService;

    public ReportApiService(CampsiteReportService campsiteReportService, ReportingService reportingService,
                            GroupService groupService, PowerSectionDao powerSectionDao,
                            PrincipalResolver principalResolver, OperationsViewDao operationsViewDao,
                            PedestalStateService pedestalStateService) {
        this.operationsViewDao = operationsViewDao;
        this.pedestalStateService = pedestalStateService;
        this.campsiteReportService = campsiteReportService;
        this.reportingService = reportingService;
        this.groupService = groupService;
        this.powerSectionDao = powerSectionDao;
        this.principalResolver = principalResolver;
    }

    @Override
    public List<ReportLineDTO> getReport(Username authUsername, String reportType, DateRange period,
                                         Long campsiteGroupId, Long powerSectionId, Integer year, Integer month,
                                         String timeResolution, List<String> pedestalCodes) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.REPORT_VIEW);
            if (reportType == null || reportType.isBlank()) {
                throw new InvalidInputException("Report type is missing");
            }
            if (month != null && (month < 1 || month > 12)) {
                throw new InvalidInputException("Month " + month + " does not exist");
            }
            if (powerSectionId != null && !powerSectionDao.existsById(powerSectionId)) {
                throw ObjectNotFoundException.of("Power section", powerSectionId);
            }

            Long siteGroupId = groupIdOf(principal, GroupType.SITE);
            Long issuerGroupId = groupIdOf(principal, GroupType.ISSUER);
            if (campsiteGroupId != null) {
                Group siteGroup = principal.getGroup(GroupType.SITE);
                if (siteGroup == null || !groupService.findManageable(siteGroup).contains(campsiteGroupId)) {
                    throw new AccessNotAllowedException("Campsite group " + campsiteGroupId
                            + " is not in the scope of " + principal.getUsername());
                }
                siteGroupId = campsiteGroupId;
                issuerGroupId = null;
            }

            switch (normalizedReportType(reportType)) {
                case AVERAGE_PER_HOUR_OF_DAY:
                    requirePeriod(period);
                    return campsiteReportService.hourlyConsumptionProfile(period.from(), period.to(),
                            siteGroupId, issuerGroupId, powerSectionId);
                case AVERAGE_PER_DAY_OF_WEEK:
                    requirePeriod(period);
                    return campsiteReportService.weekdayConsumptionProfile(period.from(), period.to(),
                            siteGroupId, issuerGroupId, powerSectionId);
                case MONTHLY_CONSUMPTION:
                    return campsiteReportService.getMonthlyConsumptions(siteGroupId, issuerGroupId, powerSectionId,
                            year, month);
                case QUARTERLY_CONSUMPTION:
                    return campsiteReportService.getQuarterlyConsumptions(siteGroupId, issuerGroupId,
                            powerSectionId, year);
                case CONSUMPTION_PER_STAY_NIGHT:
                    requirePeriod(period);
                    return campsiteReportService.getConsumptionPerStayNight(period.from(), period.to(), siteGroupId,
                            issuerGroupId, powerSectionId);
                case ACCUMULATED_KWH_PER_PEDESTAL:
                    requirePeriod(period);
                    return reportingService.fetchAccumulatedKwhPerPedestal(
                            ApiValues.parseEnum(TimeResolution.class, timeResolution, "time resolution"),
                            period.from(), period.to(), pedestalCodes, siteGroupId, issuerGroupId);
                case TOP_PITCHES:
                    requirePeriod(period);
                    return campsiteReportService.getTopPitches(period.from(), period.to(), siteGroupId,
                            issuerGroupId, powerSectionId);
                case SOCKET_UTILIZATION:
                    requirePeriod(period);
                    return siteGroupId != null ? socketUtilization(operationsViewDao.getSocketUtilization(siteGroupId,
                            period.from(), period.to())) : List.of();
                case SUPPLY_INCIDENTS_PER_PEDESTAL:
                    requirePeriod(period);
                    return campsiteReportService.getSupplyIncidentsPerPedestal(period.from(), period.to(),
                            siteGroupId);
                default:
                    throw new InvalidInputException("Unknown report type: " + reportType);
            }
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional
    public void refreshUtilization(Username authUsername) {
        try {
            principalResolver.resolve(authUsername, UserrightName.REPORT_VIEW);
            pedestalStateService.refreshSocketUtilization();
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    /**
     * Heatmap lines: period = day of week, series = hour "07", hours = supplying hours (2 decimals), count =
     * monitored socket minutes (rounded up).
     */
    static List<ReportLineDTO> socketUtilization(List<OperationsViewDao.UtilizationCell> cells) {
        List<ReportLineDTO> lines = new ArrayList<>(cells.size());
        for (OperationsViewDao.UtilizationCell cell : cells) {
            lines.add(new ReportLineDTO(DayOfWeek.of(cell.dayOfWeek()).name(),
                    String.format("%02d", cell.hour()), null, (cell.monitoredSeconds() + 59) / 60,
                    BigDecimal.valueOf(cell.supplyingSeconds())
                            .divide(SECONDS_PER_HOUR, HOURS_SCALE, RoundingMode.HALF_UP)));
        }
        return lines;
    }

    private static void requirePeriod(DateRange period) {
        if (period == null || period.from() == null || period.to() == null) {
            throw new InvalidInputException("Period of the report is missing");
        }
    }

    private static Long groupIdOf(Userinfo principal, GroupType type) {
        Group group = principal.getGroup(type);
        return group != null ? group.getId() : null;
    }

    /**
     * The same reading as the enum values of the calls: blanks and dashes as underscores, case ignored.
     */
    private static String normalizedReportType(String reportType) {
        return reportType.trim().replace(' ', '_').replace('-', '_').toUpperCase(Locale.ROOT);
    }
}
