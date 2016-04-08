package io.pillopl.issuer;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class IssuerSyncStatusTest {

    static final LocalDateTime NIGHT = LocalDateTime.of(2026, 7, 10, 2, 0);

    final IssuerSyncStatus status = new IssuerSyncStatus(null, IssuerSyncStatus.Direction.RECORD_EXPORT);

    @Test
    void failedRunKeepsTheWatermarkOfTheLastSuccess() {
        status.succeeded(NIGHT, 14, 2, "2 records refused");

        status.failed(NIGHT.plusDays(1), "Partner not reachable");

        assertThat(status.getStatus()).isEqualTo(IssuerSyncStatus.Status.ERROR);
        assertThat(status.getLastRun()).isEqualTo(NIGHT.plusDays(1));
        assertThat(status.getLastSuccess()).isEqualTo(NIGHT);
        assertThat(status.getItemsAccepted()).isZero();
        assertThat(status.getItemsRejected()).isZero();
        assertThat(status.getMessage()).isEqualTo("Partner not reachable");
    }

    @Test
    void successfulRunAfterAFailureClearsTheMessage() {
        status.failed(NIGHT, "Partner not reachable");

        status.succeeded(NIGHT.plusDays(1), 3, 0, null);

        assertThat(status.getStatus()).isEqualTo(IssuerSyncStatus.Status.OK);
        assertThat(status.getLastSuccess()).isEqualTo(NIGHT.plusDays(1));
        assertThat(status.getMessage()).isNull();
    }

    @Test
    void longMessageIsCutToTheColumn() {
        status.failed(NIGHT, "x".repeat(IssuerSyncStatus.MESSAGE_MAX_LENGTH + 20));

        assertThat(status.getMessage()).hasSize(IssuerSyncStatus.MESSAGE_MAX_LENGTH);
    }
}
