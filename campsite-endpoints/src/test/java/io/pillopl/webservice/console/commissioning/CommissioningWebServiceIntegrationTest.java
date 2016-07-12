package io.pillopl.webservice.console.commissioning;

import com.jayway.jsonpath.JsonPath;
import io.pillopl.dto.infrastructure.LocationDTO;
import io.pillopl.dto.infrastructure.PedestalImportDTO;
import io.pillopl.dto.infrastructure.SocketImportDTO;
import io.pillopl.group.Group;
import io.pillopl.group.Market;
import io.pillopl.group.attribute.SupplierMarkerService;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.user.Userinfo;
import io.pillopl.user.UserinfoService;
import io.pillopl.user.UserroleName;
import io.pillopl.webservice.console.WebServiceIntegrationTest;
import io.pillopl.webservice.console.common.WebServicePaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommissioningWebServiceIntegrationTest extends WebServiceIntegrationTest {

    static final String PATH = WebServicePaths.COMMISSIONING;

    @Autowired SupplierMarkerService supplierMarkerService;
    @Autowired UserinfoService userinfoService;
    @Autowired InfrastructureFixture fixture;

    Group stock;
    Pedestal inStock;

    @BeforeEach
    void setUp() {
        stock = supplierMarkerService.createSupplierStock(InfrastructureFixture.MANUFACTURER, Market.HR);
        inStock = fixture.pedestal("ACME-0001", stock, 2);
        console.user("ivo", UserroleName.SERVICE_TECHNICIAN, console.staff("HR STAFF"), console.staff("HR STAFF"),
                console.site("HR SITES"));
        Userinfo marko = console.user("marko", null, console.staff("HR SELF SERVICE"));
        userinfoService.setupAffiliateOwner(marko);
        console.user("reception", UserroleName.RECEPTION, console.staff("HR RECEPTION"), console.staff("HR RECEPTION"));
    }

    @Test
    void installerSeesTheSupplierStock() throws Exception {
        call("ivo", PATH + "/getSupplierStock", new GetSupplierStockRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pedestals[*].pedestalCode", contains("ACME-0001")))
                .andExpect(jsonPath("$.pedestals[0].numberOfSockets").value(2));
        call("reception", PATH + "/getSupplierStock", new GetSupplierStockRequest())
                .andExpect(status().isForbidden());
    }

    @Test
    void preparedCommissioningIsFoundByTokenAndMountsThePedestals() throws Exception {
        String body = call("ivo", PATH + "/createCommissioning", new CreateCommissioningRequest("marko", location(),
                "two pedestals at the lake", true, "NETWORK_ACCESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commissioning.ownerUsername").value("marko"))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.commissioning.token");

        call("ivo", PATH + "/findCommissionings", new FindCommissioningsRequest())
                .andExpect(jsonPath("$.commissionings", hasSize(1)));
        call("ivo", PATH + "/findCommissioningByToken", new FindCommissioningByTokenRequest(token.toUpperCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commissioning.accessMode").value("NETWORK_ACCESS"));

        call("ivo", PATH + "/commissionPedestals", new CommissionPedestalsRequest(token,
                List.of(inStock.getId(), 999_999L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.failedPedestalIds", contains(999_999)));
        assertThat(inStock.getGroup().getName()).isEqualTo("AFFILIATE marko");
        assertThat(inStock.getSockets().get(0).getSocketCode()).isEqualTo("HRMARKO*ACME-0001*1");
        call("ivo", PATH + "/findCommissioningByToken", new FindCommissioningByTokenRequest(token))
                .andExpect(status().isNotFound());
    }

    @Test
    void commissioningForAnOwnerNotSetUpIsAConflict() throws Exception {
        console.user("nina", null, console.staff("HR SELF SERVICE"));

        call("ivo", PATH + "/createCommissioning", new CreateCommissioningRequest("nina", location(), null, false,
                null))
                .andExpect(status().isConflict());
        call("ivo", PATH + "/createCommissioning", new CreateCommissioningRequest("ghost", location(), null, false,
                null))
                .andExpect(status().isNotFound());
        call("ivo", PATH + "/findCommissioningByToken", new FindCommissioningByTokenRequest("no-token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void directlyCommissionedPedestalGoesBackOnceToTheSupplier() throws Exception {
        call("ivo", PATH + "/commissionPedestal", new CommissionPedestalRequest("ACME-0001", "marko", location(),
                false, "house cards only"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commissioned").value(true));
        call("ivo", PATH + "/commissionPedestal", new CommissionPedestalRequest("ACME-0001", "marko", location(),
                false, "HOUSE_CARDS_ONLY"))
                .andExpect(status().isConflict());

        call("marko", PATH + "/decommissionPedestal", new DecommissionPedestalRequest(inStock.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decommissioned").value(true));
        assertThat(inStock.getGroup()).isEqualTo(stock);
        call("marko", PATH + "/decommissionPedestal", new DecommissionPedestalRequest(inStock.getId()))
                .andExpect(status().isConflict());
    }

    @Test
    void deliveryIsImportedIntoTheSupplierStock() throws Exception {
        call("ivo", PATH + "/importSupplierPedestals", new ImportSupplierPedestalsRequest(false,
                List.of(delivery("ACME-0100", "V2"), delivery("ACME-0101", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pedestalCodes", contains("ACME-0100", "ACME-0101")));
        call("ivo", PATH + "/getSupplierStock", new GetSupplierStockRequest())
                .andExpect(jsonPath("$.pedestals", hasSize(3)));
        call("ivo", PATH + "/importSupplierPedestals", new ImportSupplierPedestalsRequest(false,
                List.of(delivery("ACME-0102", "V7"))))
                .andExpect(status().isBadRequest());
        call("ivo", PATH + "/importSupplierPedestals", new ImportSupplierPedestalsRequest(false, List.of()))
                .andExpect(status().isBadRequest());
    }

    private static PedestalImportDTO delivery(String externalId, String linkVersion) {
        return new PedestalImportDTO(null, null, externalId, false, true, InfrastructureFixture.MANUFACTURER, "P4",
                linkVersion, "SN-" + externalId, null, null, null, null, "Bay campsite", "hrbay", "Obala", "12",
                "52210", "Rovinj", "hr", null,
                List.of(new SocketImportDTO(null, 1, externalId + "-S1", "CEE_BLUE_16A", 1, 16, null)));
    }

    private static LocationDTO location() {
        return new LocationDTO(null, "Marko's campsite", "hrmarko", "Lungomare", "3", "52100", "Pula", null, "hr",
                null, null, null, null);
    }
}
