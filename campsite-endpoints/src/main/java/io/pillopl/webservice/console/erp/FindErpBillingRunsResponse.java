package io.pillopl.webservice.console.erp;

import io.pillopl.dto.erp.ErpBillingRunDTO;

import java.io.Serializable;
import java.util.List;

/**
 * The billing runs, newest period first.
 */
public record FindErpBillingRunsResponse(List<ErpBillingRunDTO> runs) implements Serializable {

    public FindErpBillingRunsResponse {
        runs = runs != null ? List.copyOf(runs) : List.of();
    }
}
