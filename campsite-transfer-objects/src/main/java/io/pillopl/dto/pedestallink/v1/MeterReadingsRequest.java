package io.pillopl.dto.pedestallink.v1;

import java.io.Serializable;
import java.util.List;

/**
 * Link v1: meter readings of a socket, oldest first, during a supply (transaction id) or between supplies.
 */
public record MeterReadingsRequest(Integer socketNo, Integer transactionId, List<MeterReading> readings)
        implements Serializable {
}
