package io.pillopl.supply;

import io.pillopl.supply.SupplyIncident.IncidentType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SupplyIncidentTest {

    static final LocalDateTime LOGGED = LocalDateTime.of(2026, 8, 1, 7, 15);

    @Test
    void newIncidentIsNotReviewed() {
        SupplyIncident incident = new SupplyIncident(IncidentType.NO_TRANSACTION_ID, "HR-BAY-P001",
                "No transaction id", LOGGED);

        assertThat(incident.isReviewed()).isFalse();
        assertThat(incident.getReviewComment()).isNull();
    }

    @Test
    void blankReviewCommentIsNotKept() {
        SupplyIncident incident = new SupplyIncident(IncidentType.NO_SESSION, "HR-BAY-P001", "No session", LOGGED);

        incident.review("service", LOGGED.plusDays(1), "   ");

        assertThat(incident.isReviewed()).isTrue();
        assertThat(incident.getReviewComment()).isNull();
        assertThat(incident.getReviewDate()).isEqualTo(LOGGED.plusDays(1));
    }

    @Test
    void secondReviewNamesTheFirstReviewer() {
        SupplyIncident incident = new SupplyIncident(IncidentType.SESSION_ALREADY_CLOSED, "HR-BAY-P001",
                "Session closed", LOGGED);
        incident.review("service", LOGGED.plusHours(2), "checked");

        assertThatThrownBy(() -> incident.review("desk", LOGGED.plusHours(3), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("reviewed by service");
        assertThat(incident.getReviewComment()).isEqualTo("checked");
    }
}
