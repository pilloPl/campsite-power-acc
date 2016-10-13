package io.pillopl.webservice.console.erp;

import io.pillopl.dto.erp.ErpBillingLineDTO;

import java.io.Serializable;
import java.util.List;

/**
 * Lines of a period by account and article (a preview's lines have no id).
 */
public record ErpBillingLinesResponse(List<ErpBillingLineDTO> lines) implements Serializable {

    public ErpBillingLinesResponse {
        lines = lines != null ? List.copyOf(lines) : List.of();
    }
}
