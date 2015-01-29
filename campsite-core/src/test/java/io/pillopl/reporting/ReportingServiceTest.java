package io.pillopl.reporting;

import io.pillopl.dto.report.ReportLineDTO;
import io.pillopl.persistence.dao.ConsumptionReportDao;
import io.pillopl.reporting.ReportingService.TimeResolution;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportingServiceTest {

    static final Long SITE = 10L;
    static final List<String> PEDESTALS = List.of("P1", "P2");

    final ConsumptionReportDao dao = mock(ConsumptionReportDao.class);
    final ReportingService service = new ReportingService(dao);

    @Test
    void everySelectedPedestalHasALineInEveryMonthAndTheMonthHasItsTotal() {
        when(dao.sumKwhPerPedestal(eq(month(7)), eq(month(8)), any(), eq(SITE), eq(null)))
                .thenReturn(rows(new Object[]{"P1", new BigDecimal("12.5"), 2L},
                        new Object[]{"P2", new BigDecimal("7.25"), 1L}));
        when(dao.sumKwhPerPedestal(eq(month(8)), eq(month(9)), any(), eq(SITE), eq(null)))
                .thenReturn(rows(new Object[]{"P2", new BigDecimal("3"), 1L}));

        List<ReportLineDTO> lines = service.fetchAccumulatedKwhPerPedestal(TimeResolution.MONTH,
                LocalDateTime.of(2026, 7, 15, 10, 0), LocalDateTime.of(2026, 8, 31, 23, 59), PEDESTALS, SITE, null);

        assertThat(lines).extracting(ReportLineDTO::period, ReportLineDTO::series, ReportLineDTO::kwh,
                ReportLineDTO::count).containsExactly(
                tuple("2026_07", "P1", new BigDecimal("12.500"), 2L),
                tuple("2026_07", "P2", new BigDecimal("7.250"), 1L),
                tuple("2026_07", ReportLineDTO.ALL_SERIES, new BigDecimal("19.750"), 3L),
                tuple("2026_08", "P1", new BigDecimal("0.000"), 0L),
                tuple("2026_08", "P2", new BigDecimal("3.000"), 1L),
                tuple("2026_08", ReportLineDTO.ALL_SERIES, new BigDecimal("3.000"), 1L));
    }

    @Test
    void quartersStartAtTheQuarterOfTheFirstDayAndEndExclusive() {
        List<ReportLineDTO> lines = service.fetchAccumulatedKwhPerPedestal(TimeResolution.QUARTER,
                LocalDateTime.of(2026, 5, 20, 0, 0), LocalDateTime.of(2026, 7, 1, 0, 0), List.of("P1"), SITE, null);

        assertThat(lines).extracting(ReportLineDTO::period).containsExactly("2026_Q2", "2026_Q2", "2026_Q3",
                "2026_Q3");
        verify(dao).sumKwhPerPedestal(eq(LocalDate.of(2026, 4, 1).atStartOfDay()),
                eq(LocalDate.of(2026, 7, 1).atStartOfDay()), any(), eq(SITE), eq(null));
        verify(dao).sumKwhPerPedestal(eq(LocalDate.of(2026, 7, 1).atStartOfDay()),
                eq(LocalDate.of(2026, 10, 1).atStartOfDay()), any(), eq(SITE), eq(null));
    }

    @Test
    void yearsAreLabelledByTheYear() {
        List<ReportLineDTO> lines = service.fetchAccumulatedKwhPerPedestal(TimeResolution.YEAR,
                LocalDateTime.of(2025, 3, 1, 0, 0), LocalDateTime.of(2026, 2, 1, 0, 0), List.of("P1"), SITE, null);

        assertThat(lines).extracting(ReportLineDTO::period).containsExactly("2025", "2025", "2026", "2026");
    }

    @Test
    void accumulatedKwhNeedsAPedestal() {
        assertThatThrownBy(() -> service.fetchAccumulatedKwhPerPedestal(TimeResolution.MONTH,
                LocalDateTime.of(2026, 7, 1, 0, 0), LocalDateTime.of(2026, 7, 31, 0, 0), List.of(), SITE, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tooManyBucketsAreRefused() {
        assertThatThrownBy(() -> service.fetchAccumulatedKwhPerPedestal(TimeResolution.MONTH,
                LocalDateTime.of(2006, 1, 1, 0, 0), LocalDateTime.of(2026, 7, 31, 0, 0), PEDESTALS, SITE, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("periods");
    }

    @Test
    void periodEndingBeforeItsStartIsRefused() {
        assertThatThrownBy(() -> service.fetchAccumulatedKwhPerPedestal(TimeResolution.MONTH,
                LocalDateTime.of(2026, 8, 1, 0, 0), LocalDateTime.of(2026, 7, 1, 0, 0), PEDESTALS, SITE, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static LocalDateTime month(int month) {
        return LocalDate.of(2026, month, 1).atStartOfDay();
    }

    private static List<Object[]> rows(Object[]... rows) {
        return new ArrayList<>(List.of(rows));
    }
}
