package io.pillopl.webservice.console.qrpayconsole;

import java.io.Serializable;

/**
 * QR payments requested in [from, to] (dates or date-times, open ends allowed), optionally of one state and one
 * campsite, one page (page from 0, default the first 50).
 */
public record FindQrPayProcessesRequest(String state, Long campsiteGroupId, String from, String to, Integer page,
                                        Integer rowsPerPage) implements Serializable {
}
