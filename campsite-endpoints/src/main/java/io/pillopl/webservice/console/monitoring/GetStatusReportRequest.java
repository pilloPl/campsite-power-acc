package io.pillopl.webservice.console.monitoring;

import java.io.Serializable;

/**
 * Status report of a month ("2026-07" or any day of it, default the current month), optionally of one campsite group.
 */
public record GetStatusReportRequest(String month, Long campsiteGroupId) implements Serializable {
}
