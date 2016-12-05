package io.pillopl.infrastructure;

import io.pillopl.dto.map.CampsiteMapDTO;
import io.pillopl.dto.map.MapAreaDTO;
import io.pillopl.dto.map.MapPedestalDTO;
import io.pillopl.group.Group;
import io.pillopl.group.GroupType;
import io.pillopl.persistence.dao.OperationsViewDao.MapSocketRow;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MapApiServiceTest {

    @Test
    void centreOfACampsiteIgnoresPedestalsWithoutCoordinates() {
        List<MapPedestalDTO> pedestals = List.of(pedestal("45.1", "13.6"), pedestal("45.3", "13.8"),
                pedestal(null, null));

        assertThat(MapApiService.centre(pedestals, true)).isEqualByComparingTo("45.2");
        assertThat(MapApiService.centre(pedestals, false)).isEqualByComparingTo("13.7");
        assertThat(MapApiService.centre(List.of(pedestal(null, null)), true)).isNull();
    }

    @Test
    void rowsAreGroupedByCampsiteAndPedestalAndAPedestalWithoutSocketsHasNone() {
        List<MapSocketRow> rows = List.of(row(1L, 10L, 100L), row(1L, 10L, 101L), row(1L, 11L, null), row(2L, 20L, 200L));
        Group areasOnly = new Group(GroupType.SITE, "AREAS ONLY", "Areas only");
        areasOnly.setId(3L);
        MapAreaDTO outline = new MapAreaDTO(9L, "CAMPSITE", "Areas only", null, null, "{}");

        List<CampsiteMapDTO> campsites = MapApiService.campsites(rows, Map.of(3L, List.of(outline)),
                List.of(areasOnly));

        assertThat(campsites).extracting(CampsiteMapDTO::groupId).containsExactly(1L, 2L, 3L);
        assertThat(campsites.get(0).pedestals()).extracting(MapPedestalDTO::id).containsExactly(10L, 11L);
        assertThat(campsites.get(0).pedestals().get(0).sockets()).hasSize(2);
        assertThat(campsites.get(0).pedestals().get(1).sockets()).isEmpty();
        assertThat(campsites.get(2).areas()).containsExactly(outline);
    }

    private static MapPedestalDTO pedestal(String latitude, String longitude) {
        return new MapPedestalDTO(1L, "P", latitude != null ? new BigDecimal(latitude) : null,
                longitude != null ? new BigDecimal(longitude) : null, "P4", "V1", true, false, null, List.of());
    }

    private static MapSocketRow row(Long groupId, Long pedestalId, Long socketId) {
        return new MapSocketRow(groupId, "G" + groupId, "Group " + groupId, pedestalId, "P" + pedestalId, null, null,
                "P4", "V1", true, false, null, socketId, socketId != null ? 1 : null, "S", null, null, "AVAILABLE",
                null, null, null);
    }
}
