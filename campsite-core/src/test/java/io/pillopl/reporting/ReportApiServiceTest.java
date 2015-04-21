package io.pillopl.reporting;

import io.pillopl.api.ReportApi;
import io.pillopl.api.exception.AccessNotAllowedException;
import io.pillopl.api.exception.InvalidInputException;
import io.pillopl.api.exception.ObjectNotFoundException;
import io.pillopl.commons.DateRange;
import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.GroupType;
import io.pillopl.persistence.dao.PowerSectionDao;
import io.pillopl.reporting.ReportingService.TimeResolution;
import io.pillopl.user.PrincipalResolver;
import io.pillopl.user.Userinfo;
import io.pillopl.user.UserrightName;
import io.pillopl.value.Username;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReportApiServiceTest {

    static final Username CALLER = Username.of("owner");
    static final LocalDateTime FROM = LocalDateTime.of(2026, 7, 1, 0, 0);
    static final LocalDateTime TO = LocalDateTime.of(2026, 7, 31, 23, 59);
    static final DateRange JULY = new DateRange(FROM, TO);

    final CampsiteReportService campsiteReportService = mock(CampsiteReportService.class);
    final ReportingService reportingService = mock(ReportingService.class);
    final GroupService groupService = mock(GroupService.class);
    final PowerSectionDao powerSectionDao = mock(PowerSectionDao.class);
    final PrincipalResolver principalResolver = mock(PrincipalResolver.class);
    final ReportApiService api = new ReportApiService(campsiteReportService, reportingService, groupService,
            powerSectionDao, principalResolver);

    final Userinfo principal = mock(Userinfo.class);
    final Group site = mock(Group.class);
    final Group issuer = mock(Group.class);

    @BeforeEach
    void setUp() {
        when(principalResolver.resolve(CALLER, UserrightName.REPORT_VIEW)).thenReturn(principal);
        when(site.getId()).thenReturn(10L);
        when(issuer.getId()).thenReturn(20L);
        when(principal.getGroup(GroupType.SITE)).thenReturn(site);
        when(principal.getGroup(GroupType.ISSUER)).thenReturn(issuer);
        when(groupService.findManageable(site)).thenReturn(List.of(10L, 11L));
    }

    @Test
    void reportOfTheWholeScopeReadsBothSides() {
        api.getReport(CALLER, ReportApi.AVERAGE_PER_DAY_OF_WEEK, JULY, null, null, null, null, null, null);

        verify(campsiteReportService).getAverageConsumptionPerDayOfWeek(FROM, TO, 10L, 20L, null);
    }

    @Test
    void campsiteOfTheSiteSubtreeReplacesTheScopeAndDropsTheIssuerSide() {
        when(powerSectionDao.existsById(5L)).thenReturn(true);

        api.getReport(CALLER, "average-per-hour-of-day", JULY, 11L, 5L, null, null, null, null);

        verify(campsiteReportService).getAverageConsumptionPerHourOfDay(FROM, TO, 11L, null, 5L);
    }

    @Test
    void campsiteOutsideTheSiteSubtreeIsRefused() {
        assertThatThrownBy(() -> api.getReport(CALLER, ReportApi.MONTHLY_CONSUMPTION, null, 99L, null, null, null,
                null, null)).isInstanceOf(AccessNotAllowedException.class);
    }

    @Test
    void callerWithoutSiteGroupCannotNameACampsite() {
        when(principal.getGroup(GroupType.SITE)).thenReturn(null);

        assertThatThrownBy(() -> api.getReport(CALLER, ReportApi.MONTHLY_CONSUMPTION, null, 11L, null, null, null,
                null, null)).isInstanceOf(AccessNotAllowedException.class);
    }

    @Test
    void monthlyAndQuarterlyConsumptionNeedNoPeriod() {
        api.getReport(CALLER, ReportApi.MONTHLY_CONSUMPTION, null, null, null, 2026, 7, null, null);
        api.getReport(CALLER, ReportApi.QUARTERLY_CONSUMPTION, null, null, null, 2026, null, null, null);

        verify(campsiteReportService).getMonthlyConsumptions(10L, 20L, null, 2026, 7);
        verify(campsiteReportService).getQuarterlyConsumptions(10L, 20L, null, 2026);
    }

    @Test
    void stayNightAccumulatedAndIncidentReportsGetTheirParameters() {
        api.getReport(CALLER, ReportApi.CONSUMPTION_PER_STAY_NIGHT, JULY, null, null, null, null, null, null);
        api.getReport(CALLER, ReportApi.ACCUMULATED_KWH_PER_PEDESTAL, JULY, null, null, null, null, "quarter",
                List.of("P1"));
        api.getReport(CALLER, ReportApi.SUPPLY_INCIDENTS_PER_PEDESTAL, JULY, null, null, null, null, null, null);

        verify(campsiteReportService).getConsumptionPerStayNight(FROM, TO, 10L, 20L, null);
        verify(reportingService).fetchAccumulatedKwhPerPedestal(TimeResolution.QUARTER, FROM, TO, List.of("P1"),
                10L, 20L);
        verify(campsiteReportService).getSupplyIncidentsPerPedestal(FROM, TO, 10L);
    }

    @Test
    void reportsOverAPeriodRefuseAMissingEnd() {
        DateRange open = new DateRange(FROM, null);
        for (String type : List.of(ReportApi.AVERAGE_PER_HOUR_OF_DAY, ReportApi.AVERAGE_PER_DAY_OF_WEEK,
                ReportApi.CONSUMPTION_PER_STAY_NIGHT, ReportApi.ACCUMULATED_KWH_PER_PEDESTAL,
                ReportApi.SUPPLY_INCIDENTS_PER_PEDESTAL)) {
            assertThatThrownBy(() -> api.getReport(CALLER, type, open, null, null, null, null, "MONTH",
                    List.of("P1"))).isInstanceOf(InvalidInputException.class);
        }
        verifyNoInteractions(campsiteReportService, reportingService);
    }

    @Test
    void missingOrUnknownReportTypeIsInvalid() {
        assertThatThrownBy(() -> api.getReport(CALLER, " ", JULY, null, null, null, null, null, null))
                .isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> api.getReport(CALLER, "TOP_PITCHES", JULY, null, null, null, null, null, null))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void monthOutsideTheYearAndUnknownSectionAreRefused() {
        assertThatThrownBy(() -> api.getReport(CALLER, ReportApi.MONTHLY_CONSUMPTION, null, null, null, 2026, 0,
                null, null)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> api.getReport(CALLER, ReportApi.MONTHLY_CONSUMPTION, null, null, 7L, null, null,
                null, null)).isInstanceOf(ObjectNotFoundException.class);
    }

    @Test
    void accumulatedKwhNeedsAKnownTimeResolution() {
        assertThatThrownBy(() -> api.getReport(CALLER, ReportApi.ACCUMULATED_KWH_PER_PEDESTAL, JULY, null, null,
                null, null, "WEEK", List.of("P1"))).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> api.getReport(CALLER, ReportApi.ACCUMULATED_KWH_PER_PEDESTAL, JULY, null, null,
                null, null, null, List.of("P1"))).isInstanceOf(InvalidInputException.class);
    }
}
