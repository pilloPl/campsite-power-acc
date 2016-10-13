package io.pillopl.erp;

import io.pillopl.consumption.ExportStatus;
import io.pillopl.dto.erp.ErpBillingRunDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ErpBillingApiServiceTest {

    static final LocalDateTime JULY_RUN = LocalDateTime.of(2026, 8, 2, 4, 0);

    @Test
    void runOfAPeriodSumsItsExportStatesAndKeepsTheFirstCreation() {
        List<Object[]> summaries = List.of(
                new Object[] {"2026_07", ExportStatus.NEW, 3L, new BigDecimal("30.00"), JULY_RUN.plusMinutes(5)},
                new Object[] {"2026_07", ExportStatus.ERROR, 1L, new BigDecimal("12.50"), JULY_RUN},
                new Object[] {"2026_06", ExportStatus.PROCESSED, 2L, new BigDecimal("80.00"), JULY_RUN.minusMonths(1)});

        List<ErpBillingRunDTO> runs = ErpBillingApiService.toRuns(summaries, List.<Object[]>of(
                new Object[] {"2026_07", 2L}, new Object[] {"2026_06", 1L}));

        assertThat(runs).extracting(ErpBillingRunDTO::billingPeriod).containsExactly("2026_07", "2026_06");
        ErpBillingRunDTO july = runs.get(0);
        assertThat(july.lines()).isEqualTo(4);
        assertThat(july.accounts()).isEqualTo(2);
        assertThat(july.amount()).isEqualByComparingTo("42.50");
        assertThat(july.runDate()).isEqualTo(JULY_RUN);
        assertThat(july.linesPerExportStatus()).containsEntry("NEW", 3).containsEntry("ERROR", 1);
    }

    @Test
    void periodWithoutAccountCountShowsNoAccounts() {
        List<ErpBillingRunDTO> runs = ErpBillingApiService.toRuns(List.<Object[]>of(
                new Object[] {"2026_05", ExportStatus.NEW, 1L, new BigDecimal("9.00"), JULY_RUN}), List.of());

        assertThat(runs).singleElement().satisfies(run -> assertThat(run.accounts()).isZero());
    }
}
