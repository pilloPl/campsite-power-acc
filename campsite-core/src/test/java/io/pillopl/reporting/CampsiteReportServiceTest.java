package io.pillopl.reporting;

import io.pillopl.config.SystemPropertyService;
import io.pillopl.dto.report.ReportLineDTO;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.persistence.dao.ConsumptionReportDao;
import io.pillopl.persistence.dao.ConsumptionReportDao.ReportRecord;
import io.pillopl.persistence.dao.PedestalDao;
import io.pillopl.persistence.dao.SupplyIncidentDao;
import io.pillopl.supply.SupplyIncident;
import io.pillopl.supply.SupplyIncident.IncidentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CampsiteReportServiceTest {

    // 2026-07-10 is a Friday
    static final LocalDateTime FRIDAY = LocalDateTime.of(2026, 7, 10, 0, 0);
    static final LocalDateTime FROM = FRIDAY.minusDays(7);
    static final LocalDateTime TO = FRIDAY.plusDays(7);
    static final Long SITE = 10L;

    final ConsumptionReportDao dao = mock(ConsumptionReportDao.class);
    final SupplyIncidentDao incidentDao = mock(SupplyIncidentDao.class);
    final PedestalDao pedestalDao = mock(PedestalDao.class);
    final SystemPropertyService systemPropertyService = mock(SystemPropertyService.class);
    final CampsiteReportService service = new CampsiteReportService(dao, incidentDao, pedestalDao,
            systemPropertyService);

    @Test
    void supplyOverSeveralHoursIsSpreadOverTheHoursByTime() {
        records(record("S1", FRIDAY.plusHours(10).plusMinutes(30), FRIDAY.plusHours(12).plusMinutes(30), "4"));

        List<ReportLineDTO> lines = perHour();

        assertThat(lines).hasSize(24);
        assertThat(lines.get(0).period()).isEqualTo("00");
        assertThat(lines.get(10).kwh()).isEqualByComparingTo("1");
        assertThat(lines.get(11).kwh()).isEqualByComparingTo("2");
        assertThat(lines.get(12).kwh()).isEqualByComparingTo("1");
        assertThat(lines.get(12).count()).isEqualTo(1);
        assertThat(lines.get(13).kwh()).isEqualByComparingTo("0");
        assertThat(lines.get(13).count()).isZero();
    }

    @Test
    void hourAverageIsTheHourTotalDividedByTheSocketsThatDrewEnergyInIt() {
        records(record("S1", FRIDAY.plusHours(10), FRIDAY.plusHours(11), "2"),
                record("S2", FRIDAY.plusHours(10), FRIDAY.plusHours(11), "4"),
                record("S2", FRIDAY.plusDays(1).plusHours(10), FRIDAY.plusDays(1).plusHours(11), "3"));

        ReportLineDTO ten = perHour().get(10);

        // (2 + 4 + 3) kWh drawn by two sockets
        assertThat(ten.kwh()).isEqualByComparingTo("4.5");
        assertThat(ten.count()).isEqualTo(2);
    }

    @Test
    void recordWithoutEnergyDoesNotLowerTheHourAverage() {
        records(record("S1", FRIDAY.plusHours(10), FRIDAY.plusHours(11), "3"),
                record("S2", FRIDAY.plusHours(10), FRIDAY.plusHours(11), "0"));

        ReportLineDTO ten = perHour().get(10);

        assertThat(ten.kwh()).isEqualByComparingTo("3");
        assertThat(ten.count()).isEqualTo(1);
    }

    @Test
    void recordWithoutSupplyTimeOrPlugInIsLeftOutOfTheHourReport() {
        records(record("S1", FRIDAY.plusHours(10), FRIDAY.plusHours(10), "5"),
                record("S2", FRIDAY.plusHours(11), FRIDAY.plusHours(10), "5"),
                record("S3", null, FRIDAY.plusHours(10), "5"));

        assertThat(perHour()).allSatisfy(line -> {
            assertThat(line.kwh()).isEqualByComparingTo("0");
            assertThat(line.count()).isZero();
        });
    }

    @Test
    void supplyOverMidnightIsSpreadOverBothDays() {
        records(record("S1", FRIDAY.plusHours(22), FRIDAY.plusHours(26), "4"));

        List<ReportLineDTO> lines = perDay();

        assertThat(lines).extracting(ReportLineDTO::period).containsExactly("MONDAY", "TUESDAY", "WEDNESDAY",
                "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY");
        assertThat(lines.get(4).kwh()).isEqualByComparingTo("2");
        assertThat(lines.get(5).kwh()).isEqualByComparingTo("2");
        assertThat(lines.get(5).count()).isEqualTo(1);
        assertThat(lines.get(6).kwh()).isEqualByComparingTo("0");
    }

    @Test
    void dayAverageCountsASocketOnceAndOnlyWhenItDrewEnergyThatDay() {
        records(record("S1", FRIDAY.plusHours(8), FRIDAY.plusHours(9), "1"),
                record("S1", FRIDAY.plusHours(18), FRIDAY.plusHours(19), "2"),
                record("S2", FRIDAY.plusHours(8), FRIDAY.plusHours(9), "3"),
                record("S3", FRIDAY.plusHours(8), FRIDAY.plusHours(9), "0"));

        ReportLineDTO friday = perDay().get(4);

        assertThat(friday.kwh()).isEqualByComparingTo("3");
        assertThat(friday.count()).isEqualTo(2);
    }

    @Test
    void recordWithoutSupplyTimeIsLeftOutOfTheDayReport() {
        records(record("S1", FRIDAY.plusHours(10), FRIDAY.plusHours(10), "5"),
                record("S2", FRIDAY.plusHours(10), FRIDAY.plusHours(11), null));

        assertThat(perDay()).allSatisfy(line -> assertThat(line.count()).isZero());
    }

    @Test
    void kwhPerStayNightIsTheEnergyDividedByTheNights() {
        when(dao.getConsumptionPerStayNight(FROM, TO, SITE, null, null)).thenReturn(List.of(
                new ReportLineDTO("BAY", null, new BigDecimal("100"), 3, null)));

        List<ReportLineDTO> lines = service.getConsumptionPerStayNight(FROM, TO, SITE, null, null);

        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).period()).isEqualTo("BAY");
        assertThat(lines.get(0).kwh()).isEqualByComparingTo("33.333");
        assertThat(lines.get(0).count()).isEqualTo(3);
    }

    @Test
    void topPitchesReportTakesAsManyRowsAsConfigured() {
        when(systemPropertyService.getReportTopPitchesMaxRows()).thenReturn(5);
        List<ReportLineDTO> top = List.of(new ReportLineDTO("A12", "BAY", new BigDecimal("80"), 4, null));
        when(dao.getTopPitches(FROM, TO, SITE, null, null, 5)).thenReturn(top);

        assertThat(service.getTopPitches(FROM, TO, SITE, null, null)).isEqualTo(top);
    }

    @Test
    void incidentsAreCountedPerTypeAndPedestalOfTheReadersCampsites() {
        Pedestal own = new Pedestal();
        own.setCode("P1");
        Pedestal other = new Pedestal();
        other.setCode("P2");
        when(pedestalDao.findPedestalsByState(null, SITE, null, null)).thenReturn(List.of(own, other));
        when(incidentDao.findByCreateDateBetweenOrderByCreateDate(FROM, TO)).thenReturn(List.of(
                incident(IncidentType.NO_SESSION, "P1"), incident(IncidentType.NO_SESSION, "P1"),
                incident(IncidentType.NO_SESSION, "P2"), incident(IncidentType.NO_TRANSACTION_ID, "P1"),
                incident(IncidentType.NO_SESSION, "FOREIGN"), incident(IncidentType.NO_SESSION, null)));

        List<ReportLineDTO> lines = service.getSupplyIncidentsPerPedestal(FROM, TO, SITE);

        assertThat(lines).extracting(ReportLineDTO::period, ReportLineDTO::series, ReportLineDTO::count)
                .containsExactly(
                        tuple("NO_SESSION", "P1", 2L),
                        tuple("NO_SESSION", "P2", 1L),
                        tuple("NO_TRANSACTION_ID", "P1", 1L));
    }

    @Test
    void readerWithoutSiteGroupSeesNoIncidents() {
        assertThat(service.getSupplyIncidentsPerPedestal(FROM, TO, null)).isEmpty();
        verifyNoInteractions(incidentDao, pedestalDao);
    }

    private List<ReportLineDTO> perHour() {
        return service.getAverageConsumptionPerHourOfDay(FROM, TO, SITE, null, null);
    }

    private List<ReportLineDTO> perDay() {
        return service.getAverageConsumptionPerDayOfWeek(FROM, TO, SITE, null, null);
    }

    private void records(ReportRecord... records) {
        when(dao.findReportRecords(any(), any(), any(), any(), any())).thenReturn(Arrays.asList(records));
    }

    private static ReportRecord record(String socket, LocalDateTime plugIn, LocalDateTime plugOut, String kwh) {
        return new ReportRecord(socket, "P1", plugIn, plugOut, kwh != null ? new BigDecimal(kwh) : null);
    }

    private static SupplyIncident incident(IncidentType type, String pedestalCode) {
        SupplyIncident incident = new SupplyIncident();
        incident.setIncidentType(type);
        incident.setPedestalCode(pedestalCode);
        return incident;
    }
}
