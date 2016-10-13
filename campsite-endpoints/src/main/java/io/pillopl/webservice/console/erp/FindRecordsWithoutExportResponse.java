package io.pillopl.webservice.console.erp;

import io.pillopl.dto.consumption.ConsumptionRecordDTO;

import java.io.Serializable;
import java.util.List;

/**
 * The records waiting for a decision, oldest plug-out first.
 */
public record FindRecordsWithoutExportResponse(List<ConsumptionRecordDTO> consumptionRecords)
        implements Serializable {

    public FindRecordsWithoutExportResponse {
        consumptionRecords = consumptionRecords != null ? List.copyOf(consumptionRecords) : List.of();
    }
}
