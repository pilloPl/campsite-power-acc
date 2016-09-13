package io.pillopl.webservice.console.qrpayconsole;

import io.pillopl.dto.qrpay.QrPayProcessOverviewDTO;

import java.io.Serializable;
import java.util.List;

/**
 * One page of QR payments, newest request first, with the total of the filter.
 */
public record FindQrPayProcessesResponse(List<QrPayProcessOverviewDTO> processes, long total, Integer page,
                                         Integer rowsPerPage) implements Serializable {

    public FindQrPayProcessesResponse {
        processes = processes != null ? List.copyOf(processes) : List.of();
    }
}
