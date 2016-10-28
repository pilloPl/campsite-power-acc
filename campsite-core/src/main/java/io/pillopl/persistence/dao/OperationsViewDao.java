package io.pillopl.persistence.dao;

import io.pillopl.commons.ListPage;
import io.pillopl.commons.Pagination;
import io.pillopl.dto.supply.SupplyIncidentDTO;
import io.pillopl.dto.supply.SupplySessionDTO;

import java.time.LocalDateTime;

/**
 * Hand-written SQL of the service tabs of the console (dashboard, supply sessions, monitoring, map), always restricted
 * to the subtree of a SITE group. Read-only; the rows come back as transfer objects. Every filter is a bound
 * parameter.
 */
public interface OperationsViewDao {

    /**
     * @param silentSince sessions silent since this time count as stale
     */
    ListPage<SupplySessionDTO> findSupplySessions(Long siteGroupId, String pedestalCode, String socketCode,
                                                  boolean activeOnly, LocalDateTime from, LocalDateTime to,
                                                  LocalDateTime silentSince, Pagination pagination);

    ListPage<SupplyIncidentDTO> findSupplyIncidents(Long siteGroupId, String pedestalCode, boolean unreviewedOnly,
                                                    LocalDateTime from, LocalDateTime to, Pagination pagination);
}
