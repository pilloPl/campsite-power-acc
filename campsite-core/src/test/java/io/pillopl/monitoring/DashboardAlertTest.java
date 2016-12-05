package io.pillopl.monitoring;

import io.pillopl.dto.monitoring.AlertDTO;
import io.pillopl.persistence.dao.OperationsViewDao.AlertSource;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DashboardAlertTest {

    static final LocalDateTime AT = LocalDateTime.of(2026, 7, 10, 18, 30);

    @Test
    void faultsAndTimeoutsAreErrorsStaleSessionsFailedEventsAndErpLinesWarningsTheReviewQueueInformation() {
        assertThat(alert("SOCKET_FAULT", "FAULTED").severity()).isEqualTo("ERROR");
        assertThat(alert("PEDESTAL_TIMEOUT", "2026-07-10 12:00").severity()).isEqualTo("ERROR");
        assertThat(alert("STALE_SESSION", "2026-07-09 10:00").severity()).isEqualTo("WARNING");
        assertThat(alert("FAILED_EVENT", "SUPPLY_STOPPED: queue full").severity()).isEqualTo("WARNING");
        assertThat(alert("ERP_ERROR", "2026_07 4000123: unknown article").severity()).isEqualTo("WARNING");
        assertThat(alert("REVIEW_RECORD", "400 kWh in one night").severity()).isEqualTo("INFO");
        assertThatThrownBy(() -> alert("LOST", null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void textNamesTheObjectsOfTheAlert() {
        assertThat(alert("SOCKET_FAULT", "BREAKER_TRIPPED").text())
                .isEqualTo("Socket P001_2 of pedestal HR-BAY-P001: BREAKER_TRIPPED");
        assertThat(alert("ERP_ERROR", "2026_07 4000123: unknown article").text())
                .isEqualTo("ERP refused the billing line 2026_07 4000123: unknown article");
    }

    @Test
    void keyNamesTypeObjectAndOccurrenceSoThatANewFaultIsANewAlert() {
        AlertDTO first = alert("SOCKET_FAULT", "FAULTED");
        AlertDTO later = DashboardApiService.toAlert(new AlertSource("SOCKET_FAULT", 17L, AT.plusDays(1),
                "HR-BAY-P001", 17L, "P001_2", null, null, "FAULTED"));

        assertThat(first.alertKey()).isEqualTo("SOCKET_FAULT:17:"
                + AT.atZone(ZoneId.systemDefault()).toEpochSecond());
        assertThat(later.alertKey()).isNotEqualTo(first.alertKey());
        assertThat(first.acknowledged()).isFalse();
    }

    @Test
    void rowLimitIsBetweenOneAndOneThousand() {
        assertThat(DashboardApiService.rows(null)).isEqualTo(200);
        assertThatThrownBy(() -> DashboardApiService.rows(0)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> DashboardApiService.rows(1001)).isInstanceOf(RuntimeException.class);
    }

    private static AlertDTO alert(String type, String detail) {
        return DashboardApiService.toAlert(new AlertSource(type, 17L, AT, "HR-BAY-P001", 17L, "P001_2", null,
                null, detail));
    }
}
