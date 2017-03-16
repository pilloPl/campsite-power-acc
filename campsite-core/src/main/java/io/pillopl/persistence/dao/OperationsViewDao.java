package io.pillopl.persistence.dao;

import io.pillopl.commons.ListPage;
import io.pillopl.commons.Pagination;
import io.pillopl.dto.map.MapSectionLoadDTO;
import io.pillopl.dto.supply.SupplyIncidentDTO;
import io.pillopl.dto.supply.SupplySessionDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

    /**
     * Key figures of the dashboard; failedEvents and erpLinesInError are null unless asked for.
     */
    record DashboardCounts(long activeSessions, BigDecimal kwhToday, long faultedSockets,
                           long breakerTrippedSockets, long timedOutPedestals, long reviewQueue, long staleSessions,
                           Long failedEvents, Long erpLinesInError) {
    }

    DashboardCounts getDashboardCounts(Long siteGroupId, LocalDateTime dayStart, LocalDateTime silentSince,
                                       boolean withEvents, boolean withErp);

    /**
     * One source of a live alert: its type, the object and the time of the occurrence, what it points to and a
     * detail for the text.
     */
    record AlertSource(String alertType, long objectId, LocalDateTime alertTime, String pedestalCode, Long socketId,
                       String socketName, Long sessionId, Long recordId, String detail) {
    }

    /**
     * A socket of the map with its pedestal and campsite (socket columns null for a pedestal without sockets).
     */
    record MapSocketRow(Long groupId, String groupName, String groupDisplayName, Long pedestalId, String pedestalCode,
                        BigDecimal latitude, BigDecimal longitude, String pedestalType, String linkVersion,
                        boolean remoteControl, boolean timeout, LocalDateTime lastIncoming, Long socketId,
                        Integer socketNo, String socketName, String socketCode, String pitchNo, String status,
                        Long powerSectionId, Long sessionId, Integer sessionMaxCurrentA) {
    }

    List<MapSocketRow> findMapSockets(Long siteGroupId);

    List<MapSectionLoadDTO> findSectionLoads(Long siteGroupId);

    /**
     * One cell of the utilization heatmap (ISO day of week 1..7, hour 0..23).
     */
    record UtilizationCell(int dayOfWeek, int hour, long monitoredSeconds, long supplyingSeconds, long sockets) {
    }

    /**
     * Cells with samples of MV SOCKET_UTILIZATION in [from, to], by day of week and hour.
     */
    List<UtilizationCell> getSocketUtilization(Long siteGroupId, LocalDateTime from, LocalDateTime to);

    List<AlertSource> findAlertSources(Long siteGroupId, LocalDateTime silentSince, boolean withEvents,
                                       boolean withErp, int maxRows);
}
