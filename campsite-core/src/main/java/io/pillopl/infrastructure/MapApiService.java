package io.pillopl.infrastructure;

import io.pillopl.api.MapApi;
import io.pillopl.dto.map.CampsiteMapDTO;
import io.pillopl.dto.map.MapAreaDTO;
import io.pillopl.dto.map.MapDataDTO;
import io.pillopl.dto.map.MapPedestalDTO;
import io.pillopl.dto.map.MapSocketDTO;
import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.persistence.dao.CampsiteMapAreaDao;
import io.pillopl.persistence.dao.OperationsViewDao;
import io.pillopl.persistence.dao.OperationsViewDao.MapSocketRow;
import io.pillopl.user.PrincipalResolver;
import io.pillopl.user.Userinfo;
import io.pillopl.user.UserrightName;
import io.pillopl.util.ApiExceptionTranslator;
import io.pillopl.util.SiteScopeHelper;
import io.pillopl.value.Username;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Map of the console (PEDESTAL_VIEW): campsites of the caller's SITE scope with their outlines (CAMPSITE_MAP_AREA),
 * pedestals at the coordinates of their location with the statuses of their sockets, and the section load overlay.
 * A campsite is a SITE group that holds pedestals or map areas.
 */
@Service
@Transactional(readOnly = true)
public class MapApiService implements MapApi {

    static final int COORDINATE_SCALE = 7;

    private final OperationsViewDao operationsViewDao;

    private final CampsiteMapAreaDao campsiteMapAreaDao;

    private final GroupService groupService;

    private final PrincipalResolver principalResolver;

    public MapApiService(OperationsViewDao operationsViewDao, CampsiteMapAreaDao campsiteMapAreaDao,
                         GroupService groupService, PrincipalResolver principalResolver) {
        this.operationsViewDao = operationsViewDao;
        this.campsiteMapAreaDao = campsiteMapAreaDao;
        this.groupService = groupService;
        this.principalResolver = principalResolver;
    }

    @Override
    public MapDataDTO getMapData(Username authUsername, Long campsiteGroupId) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.PEDESTAL_VIEW);
            Long siteGroupId = SiteScopeHelper.siteGroupIdOf(principal, campsiteGroupId, groupService);
            if (siteGroupId == null) {
                return new MapDataDTO(List.of(), List.of());
            }
            Group root = groupService.fetchByGroupId(siteGroupId);
            List<Group> groups = groupService.findManageableGroups(root);
            List<Long> groupIds = groups.stream().map(Group::getId).toList();
            Map<Long, List<MapAreaDTO>> areas = new LinkedHashMap<>();
            for (CampsiteMapArea area : campsiteMapAreaDao
                    .findBySiteGroupIdInOrderBySiteGroupIdAscAreaTypeAscNameAsc(groupIds)) {
                areas.computeIfAbsent(area.getSiteGroupId(), id -> new ArrayList<>()).add(new MapAreaDTO(area.getId(),
                        area.getAreaType().name(), area.getName(), area.getPitchNo(), area.getPowerSectionId(),
                        area.getGeoJson()));
            }
            return new MapDataDTO(campsites(operationsViewDao.findMapSockets(siteGroupId), areas, groups),
                    operationsViewDao.findSectionLoads(siteGroupId));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    /**
     * Groups the socket rows by campsite and pedestal (in the order of the rows); campsites with map areas but no
     * pedestals follow in the order of the group tree.
     */
    static List<CampsiteMapDTO> campsites(List<MapSocketRow> rows, Map<Long, List<MapAreaDTO>> areas,
                                          List<Group> groups) {
        Map<Long, MapSocketRow> campsiteRows = new LinkedHashMap<>();
        Map<Long, Map<Long, List<MapSocketRow>>> pedestals = new LinkedHashMap<>();
        for (MapSocketRow row : rows) {
            campsiteRows.putIfAbsent(row.groupId(), row);
            pedestals.computeIfAbsent(row.groupId(), id -> new LinkedHashMap<>())
                    .computeIfAbsent(row.pedestalId(), id -> new ArrayList<>()).add(row);
        }
        List<CampsiteMapDTO> campsites = new ArrayList<>();
        for (Map.Entry<Long, MapSocketRow> campsite : campsiteRows.entrySet()) {
            List<MapPedestalDTO> mapPedestals = new ArrayList<>();
            for (List<MapSocketRow> pedestalRows : pedestals.get(campsite.getKey()).values()) {
                mapPedestals.add(toPedestal(pedestalRows));
            }
            MapSocketRow first = campsite.getValue();
            campsites.add(new CampsiteMapDTO(first.groupId(), first.groupName(), first.groupDisplayName(),
                    centre(mapPedestals, true), centre(mapPedestals, false),
                    areas.getOrDefault(first.groupId(), List.of()), mapPedestals));
        }
        for (Group group : groups) {
            if (!campsiteRows.containsKey(group.getId()) && areas.containsKey(group.getId())) {
                campsites.add(new CampsiteMapDTO(group.getId(), group.getName(), group.getDisplayName(), null, null,
                        areas.get(group.getId()), List.of()));
            }
        }
        return campsites;
    }

    private static MapPedestalDTO toPedestal(List<MapSocketRow> rows) {
        MapSocketRow first = rows.get(0);
        List<MapSocketDTO> sockets = new ArrayList<>();
        for (MapSocketRow row : rows) {
            if (row.socketId() != null) {
                sockets.add(new MapSocketDTO(row.socketId(), row.socketNo(), row.socketName(), row.socketCode(),
                        row.pitchNo(), row.status(), row.powerSectionId(), row.sessionId(), row.sessionMaxCurrentA()));
            }
        }
        return new MapPedestalDTO(first.pedestalId(), first.pedestalCode(), first.latitude(), first.longitude(),
                first.pedestalType(), first.linkVersion(), first.remoteControl(), first.timeout(),
                first.lastIncoming(), sockets);
    }

    /**
     * Mean latitude (or longitude) of the pedestals with coordinates, null when none has them.
     */
    static BigDecimal centre(List<MapPedestalDTO> pedestals, boolean latitude) {
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (MapPedestalDTO pedestal : pedestals) {
            BigDecimal value = latitude ? pedestal.latitude() : pedestal.longitude();
            if (value != null && pedestal.latitude() != null && pedestal.longitude() != null) {
                sum = sum.add(value);
                count++;
            }
        }
        return count == 0 ? null : sum.divide(BigDecimal.valueOf(count), COORDINATE_SCALE, RoundingMode.HALF_UP);
    }
}
