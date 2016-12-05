package io.pillopl.dto.monitoring;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Key figures of the dashboard in the caller's SITE scope. {@code failedEvents} (CONFIG_EDIT) and
 * {@code erpLinesInError} (BILLING_EDIT) are network wide and null without the right. {@code kwhToday} is the
 * consumption of the records with plug-out today plus the consumption so far of the active sessions.
 */
public record DashboardDTO(LocalDateTime asOf, long activeSessions, BigDecimal kwhToday, long faultedSockets,
                           long breakerTrippedSockets, long timedOutPedestals, long reviewQueue, long staleSessions,
                           Long failedEvents, Long erpLinesInError, long openAlerts) implements Serializable {
}
