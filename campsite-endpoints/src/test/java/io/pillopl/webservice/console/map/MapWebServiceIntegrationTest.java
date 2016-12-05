package io.pillopl.webservice.console.map;

import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupType;
import io.pillopl.infrastructure.CampsiteMapArea;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Location;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.Socket;
import io.pillopl.infrastructure.SocketStatus;
import io.pillopl.persistence.dao.CampsiteMapAreaDao;
import io.pillopl.persistence.dao.PowerSectionDao;
import io.pillopl.persistence.dao.SocketDao;
import io.pillopl.persistence.dao.SupplySessionDao;
import io.pillopl.power.PowerSection;
import io.pillopl.supply.SupplySession;
import io.pillopl.user.UserroleName;
import io.pillopl.webservice.console.WebServiceIntegrationTest;
import io.pillopl.webservice.console.common.WebServicePaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MapWebServiceIntegrationTest extends WebServiceIntegrationTest {

    static final String PATH = WebServicePaths.MAP;
    static final String PITCH = "{\"type\":\"Polygon\",\"coordinates\":[[[13.63,45.08],[13.6302,45.08],"
            + "[13.6302,45.0802],[13.63,45.0802],[13.63,45.08]]]}";

    @Autowired GroupManager groupManager;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired SocketDao socketDao;
    @Autowired SupplySessionDao supplySessionDao;
    @Autowired PowerSectionDao powerSectionDao;
    @Autowired CampsiteMapAreaDao campsiteMapAreaDao;

    Group bay;
    Group dunes;
    PowerSection sectionA;

    @BeforeEach
    void setUp() {
        bay = groupManager.createChildGroup(console.site("HR SITES"), "BAY MAP CONSOLE", "Bay", GroupType.SITE);
        dunes = groupManager.createChildGroup(bay, "DUNES MAP CONSOLE", "Dunes", GroupType.SITE);
        Group lake = groupManager.createChildGroup(console.site("HR SITES"), "LAKE MAP CONSOLE", "Lake", GroupType.SITE);
        sectionA = powerSectionDao.saveAndFlush(new PowerSection("BAY MAP SECTION A", new BigDecimal("32"),
                "nominalCurrentSum"));

        Pedestal first = infrastructure.pedestal("HR-BAYMAP-P001", bay, 2, located("45.0800000", "13.6300000"));
        infrastructure.pedestal("HR-BAYMAP-P002", bay, 1, located("45.0810000", "13.6310000"));
        infrastructure.pedestal("HR-LAKEMAP-P001", lake, 1, located("46.0", "14.0"));
        Socket supplying = first.getSockets().get(0);
        supplying.setPitchNo("A1");
        supplying.setPowerSection(sectionA);
        supplying.updateStatus(SocketStatus.SUPPLYING, LocalDateTime.now());
        socketDao.saveAndFlush(supplying);
        Socket second = first.getSockets().get(1);
        second.setPowerSection(sectionA);
        socketDao.saveAndFlush(second);
        SupplySession session = new SupplySession();
        session.setSocket(supplying);
        session.setTransactionId(850_001);
        session.setCreateDate(LocalDateTime.now().minusHours(1));
        session.setMaxCurrentA(16);
        session.setActive(true);
        supplySessionDao.saveAndFlush(session);

        CampsiteMapArea pitch = new CampsiteMapArea(bay.getId(), CampsiteMapArea.AreaType.PITCH, "A1", PITCH);
        campsiteMapAreaDao.saveAndFlush(pitch);
        CampsiteMapArea section = new CampsiteMapArea(bay.getId(), CampsiteMapArea.AreaType.SECTION, "Section A",
                PITCH);
        section.setPowerSectionId(sectionA.getId());
        campsiteMapAreaDao.saveAndFlush(section);
        campsiteMapAreaDao.saveAndFlush(new CampsiteMapArea(dunes.getId(), CampsiteMapArea.AreaType.CAMPSITE,
                "Dunes", PITCH));
        console.user("baydesk", UserroleName.RECEPTION, console.staff("HR RECEPTION"), bay);
        console.user("nobody", null, console.staff("HR RECEPTION"), bay);
    }

    @Test
    void mapShowsTheCampsitesOfTheScopeWithOutlinesPedestalsAndSectionLoad() throws Exception {
        call("baydesk", PATH + "/getMapData", new GetMapDataRequest(null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.map.campsites", hasSize(2)))
                .andExpect(jsonPath("$.map.campsites[0].displayName").value("Bay"))
                .andExpect(jsonPath("$.map.campsites[0].latitude").value(45.0805))
                .andExpect(jsonPath("$.map.campsites[0].longitude").value(13.6305))
                .andExpect(jsonPath("$.map.campsites[0].areas", hasSize(2)))
                .andExpect(jsonPath("$.map.campsites[0].areas[?(@.areaType == 'PITCH')].pitchNo").value("A1"))
                .andExpect(jsonPath("$.map.campsites[0].pedestals", hasSize(2)))
                .andExpect(jsonPath("$.map.campsites[0].pedestals[0].code").value("HR-BAYMAP-P001"))
                .andExpect(jsonPath("$.map.campsites[0].pedestals[0].linkVersion").value("V1"))
                .andExpect(jsonPath("$.map.campsites[0].pedestals[0].sockets", hasSize(2)))
                .andExpect(jsonPath("$.map.campsites[0].pedestals[0].sockets[0].status").value("SUPPLYING"))
                .andExpect(jsonPath("$.map.campsites[0].pedestals[0].sockets[0].activeSessionId").isNumber())
                .andExpect(jsonPath("$.map.campsites[1].displayName").value("Dunes"))
                .andExpect(jsonPath("$.map.campsites[1].pedestals", hasSize(0)))
                .andExpect(jsonPath("$.map.sections", hasSize(1)))
                .andExpect(jsonPath("$.map.sections[0].sockets").value(2))
                .andExpect(jsonPath("$.map.sections[0].activeSessions").value(1))
                .andExpect(jsonPath("$.map.sections[0].allocatedCurrentA").value(16))
                .andExpect(jsonPath("$.map.sections[0].loadShare").value(0.5));
    }

    @Test
    void mapOfOneCampsiteAndRefusals() throws Exception {
        call("baydesk", PATH + "/getMapData", new GetMapDataRequest(dunes.getId()))
                .andExpect(jsonPath("$.map.campsites", hasSize(1)))
                .andExpect(jsonPath("$.map.sections", hasSize(0)));
        call("baydesk", PATH + "/getMapData", new GetMapDataRequest(console.site("HR SITES").getId()))
                .andExpect(status().isForbidden());
        call("nobody", PATH + "/getMapData", new GetMapDataRequest(null)).andExpect(status().isForbidden());
        call(PATH + "/getMapData", new GetMapDataRequest(null)).andExpect(status().isUnauthorized());
    }

    private Location located(String latitude, String longitude) {
        Location location = infrastructure.campsiteLocation("Bay map");
        location.setLatitude(new BigDecimal(latitude));
        location.setLongitude(new BigDecimal(longitude));
        return location;
    }
}
