package io.pillopl.webservice.console.monitoring;

import java.io.Serializable;

/**
 * Logs of the monitoring tab: the pedestal (status and communication logs), optionally one socket (status logs),
 * the campsite group (heartbeat timeouts), the period [from, to] (default the last 7 days) and at most maxRows rows
 * (default 200, at most 1000).
 */
public record FindMonitoringLogsRequest(String pedestalCode, Long socketId, Long campsiteGroupId, String from,
                                        String to, Integer maxRows) implements Serializable {
}
