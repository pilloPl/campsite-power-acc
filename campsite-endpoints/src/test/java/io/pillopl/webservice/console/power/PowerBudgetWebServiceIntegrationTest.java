package io.pillopl.webservice.console.power;

import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupType;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.Socket;
import io.pillopl.persistence.dao.PowerSectionDao;
import io.pillopl.persistence.dao.SocketDao;
import io.pillopl.power.NominalCurrentSumAlgorithm;
import io.pillopl.power.PowerSection;
import io.pillopl.user.UserroleName;
import io.pillopl.webservice.console.WebServiceIntegrationTest;
import io.pillopl.webservice.console.common.WebServicePaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PowerBudgetWebServiceIntegrationTest extends WebServiceIntegrationTest {

    static final String PATH = WebServicePaths.POWER_BUDGET;

    @Autowired GroupManager groupManager;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired SocketDao socketDao;
    @Autowired PowerSectionDao powerSectionDao;

    PowerSection section;
    Socket socket;

    @BeforeEach
    void setUp() {
        Group bay = groupManager.createChildGroup(console.site("HR SITES"), "BAY POWER TAB", "Bay", GroupType.SITE);
        Pedestal pedestal = infrastructure.pedestal("HR-BAYPW-P001", bay, 2);
        section = powerSectionDao.saveAndFlush(new PowerSection("Bay north", new BigDecimal("32"),
                NominalCurrentSumAlgorithm.BEAN_NAME));
        socket = pedestal.getSockets().get(0);
        socket.setPowerSection(section);
        socketDao.saveAndFlush(socket);
        console.user("technician", UserroleName.SERVICE_TECHNICIAN, console.staff("HR RECEPTION"), bay);
        console.user("desk", UserroleName.RECEPTION, console.staff("HR RECEPTION"), bay);
    }

    @Test
    void technicianSeesTheSectionsAndTheirLoad() throws Exception {
        call("technician", PATH + "/findPowerSections", new FindPowerSectionsRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableAlgorithms", hasItem("nominalCurrentSum")))
                .andExpect(jsonPath("$.sections[?(@.sectionName == 'Bay north')].socketCount").value(1));
        call("technician", PATH + "/getSectionLoad", new GetSectionLoadRequest(section.getId(), "2026-07-10",
                "2026-07-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.load.points.length()").value(24))
                .andExpect(jsonPath("$.load.points[0].allocatedA").value(0));
    }

    @Test
    void technicianChangesTheSectionAndReceptionMayOnlyLook() throws Exception {
        call("technician", PATH + "/changeSectionCapacity", new ChangeSectionCapacityRequest(section.getId(),
                new BigDecimal("40")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.section.maxCurrentA").value(40.0));
        call("technician", PATH + "/selectAllocationAlgorithm", new SelectAllocationAlgorithmRequest(
                section.getId(), "magic")).andExpect(status().isBadRequest());
        call("technician", PATH + "/assignSocketToSection", new AssignSocketToSectionRequest(socket.getId(), null))
                .andExpect(status().isOk());
        call("technician", PATH + "/savePowerSection", new SavePowerSectionRequest(null, "Bay south",
                new BigDecimal("25"), "nominalCurrentSum"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.section.sectionName").value("Bay south"));
        call("desk", PATH + "/findPowerSections", new FindPowerSectionsRequest()).andExpect(status().isOk());
        call("desk", PATH + "/changeSectionCapacity", new ChangeSectionCapacityRequest(section.getId(),
                new BigDecimal("50"))).andExpect(status().isForbidden());
    }
}
