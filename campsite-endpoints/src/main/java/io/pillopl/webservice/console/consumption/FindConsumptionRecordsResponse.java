package io.pillopl.webservice.console.consumption;

import io.pillopl.dto.consumption.ConsumptionRecordDTO;

import java.io.Serializable;
import java.util.List;

/**
 * The records, newest plug-out first.
 */
public record FindConsumptionRecordsResponse(List<ConsumptionRecordDTO> consumptionRecords)
        implements Serializable {
}
