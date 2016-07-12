package io.pillopl.webservice.console.pedestal;

import io.pillopl.dto.infrastructure.LocationDTO;
import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupType;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.Socket;
import io.pillopl.user.UserroleName;
import io.pillopl.webservice.console.WebServiceIntegrationTest;
import io.pillopl.webservice.console.common.WebServicePaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PedestalWebServiceIntegrationTest extends WebServiceIntegrationTest {

    static final String PATH = WebServicePaths.PEDESTAL;

    @Autowired GroupManager groupManager;
    @Autowired InfrastructureFixture fixture;

    Group bay, cove;
    Pedestal atBay;

    @BeforeEach
    void setUp() {
        bay = groupManager.createChildGroup(console.site("HR SITES"), "BAY CAMPSITE", "Bay", GroupType.SITE);
        cove = groupManager.createChildGroup(console.site("HR SITES"), "COVE CAMPSITE", "Cove", GroupType.SITE);
        console.hrAdmin("admin");
        console.user("manager", UserroleName.CAMPSITE_MANAGER, console.staff("HR RECEPTION"),
                console.staff("HR RECEPTION"), bay);
        console.user("reception", UserroleName.RECEPTION, console.staff("HR RECEPTION"), console.staff("HR RECEPTION"),
                bay);
        atBay = fixture.pedestal("HR-BAY-P001", bay, 2);
        fixture.pedestal("HR-BAY-P002", bay, 1);
        fixture.pedestal("HR-COVE-P001", cove, 1);
    }

    @Test
    void pedestalListOfTheScopeIsPagedWithTheTotal() throws Exception {
        call("manager", PATH + "/findPedestalsByState", new FindPedestalsByStateRequest(null, 0, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pedestals", hasSize(1)))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.rowsPerPage").value(1));
        call("manager", PATH + "/findPedestalsByState", new FindPedestalsByStateRequest("working", null, null))
                .andExpect(status().isOk());
        call("manager", PATH + "/findPedestalsByState", new FindPedestalsByStateRequest(null, 0, 10_000))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pedestalOutsideTheScopeIsNotFound() throws Exception {
        call("manager", PATH + "/getPedestal", new GetPedestalRequest("HR-BAY-P001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pedestal.sockets", hasSize(2)))
                .andExpect(jsonPath("$.pedestal.groupName").value("Bay"))
                .andExpect(jsonPath("$.pedestal.linkTimeout").value(true));
        call("manager", PATH + "/getPedestal", new GetPedestalRequest("HR-COVE-P001"))
                .andExpect(status().isNotFound());
        call("admin", PATH + "/getPedestal", new GetPedestalRequest("HR-COVE-P001"))
                .andExpect(status().isOk());
    }

    @Test
    void pedestalIsFoundByTheCodeOnTheSocketSticker() throws Exception {
        Socket socket = atBay.getSockets().get(1);
        socket.setSocketCode("HRBAY*P001*2");

        call("manager", PATH + "/findPedestalBySocketCode", new FindPedestalBySocketCodeRequest("hrbay*p001*2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pedestal.code").value("HR-BAY-P001"));
        call("manager", PATH + "/findPedestalBySocketCode", new FindPedestalBySocketCodeRequest("HRBAY*P001*9"))
                .andExpect(status().isNotFound());
        call("manager", PATH + "/findPedestalBySocketCode", new FindPedestalBySocketCodeRequest("HRBAY-P001-2"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pitchIsAssignedUpperCaseAndOnlyOnce() throws Exception {
        Long first = atBay.getSockets().get(0).getId();
        Long second = atBay.getSockets().get(1).getId();

        call("manager", PATH + "/assignPitch", new AssignPitchRequest(first, " a12 "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.socket.pitchNo").value("A12"));
        call("manager", PATH + "/assignPitch", new AssignPitchRequest(second, "A12"))
                .andExpect(status().isConflict());
        call("manager", PATH + "/assignPitch", new AssignPitchRequest(first, ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.socket.pitchNo").doesNotExist());
        call("manager", PATH + "/assignPitch", new AssignPitchRequest(999_999L, "A1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void receptionMayLookButNotChangeSockets() throws Exception {
        Long socketId = atBay.getSockets().get(0).getId();

        call("reception", PATH + "/getPedestal", new GetPedestalRequest("HR-BAY-P001")).andExpect(status().isOk());
        call("reception", PATH + "/setFreeSocket", new SetFreeSocketRequest(socketId, true))
                .andExpect(status().isForbidden());
        call("manager", PATH + "/setFreeSocket", new SetFreeSocketRequest(socketId, true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.socket.freeSocket").value(true));
    }

    @Test
    void adminDeletesAndMovesPedestalsOfAManagedOwner() throws Exception {
        LocationDTO newAddress = new LocationDTO(null, "Bay north", "hrbay", "Obala", "40", "52210", "Rovinj", null,
                "HR", null, null, null, null);

        call("admin", PATH + "/updateLocation", new UpdateLocationRequest("manager", "HR-BAY-P002", newAddress))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pedestal.location.houseNumber").value("40"))
                .andExpect(jsonPath("$.pedestal.location.campsiteCode").value("HRBAY"));
        call("admin", PATH + "/deletePedestal", new DeletePedestalRequest("manager", "HR-BAY-P001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(true));
        assertThat(atBay.isDeleted()).isTrue();
        call("admin", PATH + "/deletePedestal", new DeletePedestalRequest("manager", "HR-COVE-P001"))
                .andExpect(status().isForbidden());
        call("admin", PATH + "/deletePedestal", new DeletePedestalRequest("manager", "HR-NOPE"))
                .andExpect(status().isNotFound());
    }
}
