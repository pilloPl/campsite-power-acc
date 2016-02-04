package io.pillopl.webservice.pedestallink.v3;

import io.pillopl.card.CardFixture;
import io.pillopl.card.LocalCardListSender;
import io.pillopl.dto.pedestallink.v1.AuthorizeRequest;
import io.pillopl.dto.pedestallink.v1.FirmwareStatusRequest;
import io.pillopl.dto.pedestallink.v1.MeterReading;
import io.pillopl.dto.pedestallink.v1.MeterReadingsRequest;
import io.pillopl.dto.pedestallink.v1.SupplyStartedRequest;
import io.pillopl.dto.pedestallink.v1.SupplyStoppedRequest;
import io.pillopl.dto.pedestallink.v2.SocketStatusRequest;
import io.pillopl.dto.pedestallink.v2.VendorDataRequest;
import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupType;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.PedestalLinkVersion;
import io.pillopl.infrastructure.PedestalType;
import io.pillopl.infrastructure.Socket;
import io.pillopl.infrastructure.SocketStatus;
import io.pillopl.pedestallink.PedestalLinkClientDispatcher;
import io.pillopl.persistence.dao.PedestalDao;
import io.pillopl.persistence.dao.PedestalStateDao;
import io.pillopl.persistence.dao.PedestalTypeDao;
import io.pillopl.persistence.dao.SocketDao;
import io.pillopl.persistence.dao.SocketStatusLogDao;
import io.pillopl.persistence.dao.SupplySessionDao;
import io.pillopl.supply.RemoteSupplyControl;
import io.pillopl.webservice.console.WebServiceIntegrationTest;
import io.pillopl.webservice.console.common.WebServicePaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A link v3 controller: every message goes to its handler; a guest not accepted at the campsite is told so.
 */
class PedestalLinkV3WebServiceIntegrationTest extends WebServiceIntegrationTest {

    static final String PEDESTAL = "HR-BAY-P401";
    static final String PATH = WebServicePaths.PEDESTAL_LINK_V3 + "/" + PEDESTAL;

    @Autowired GroupManager groupManager;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired CardFixture cards;
    @Autowired PedestalDao pedestalDao;
    @Autowired PedestalTypeDao pedestalTypeDao;
    @Autowired PedestalStateDao pedestalStateDao;
    @Autowired SocketDao socketDao;
    @Autowired SupplySessionDao supplySessionDao;
    @Autowired SocketStatusLogDao socketStatusLogDao;
    @Autowired LocalCardListSender localCardListSender;
    @Autowired RemoteSupplyControl remoteSupplyControl;

    final LocalDateTime plugIn = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).minusHours(3);
    Pedestal pedestal;
    Group club;

    @BeforeEach
    void setUp() {
        Group bay = groupManager.createChildGroup(console.site("HR SITES"), "BAY LINK V3", "Bay", GroupType.SITE);
        club = groupManager.createChildGroup(console.issuer("HR ISSUERS"), "LINK V3 CLUB", "Club", GroupType.ISSUER);
        groupManager.addAcceptanceAgreement(club, bay);
        PedestalType adriatec = new PedestalType("ADRIATEC", "AT-3", PedestalLinkVersion.V3);
        adriatec.setMaxSockets(4);
        pedestal = infrastructure.pedestal(PEDESTAL, bay, 2);
        pedestal.setPedestalType(pedestalTypeDao.saveAndFlush(adriatec));
        pedestal.setBillable(true);
        pedestal = pedestalDao.saveAndFlush(pedestal);
        cards.card("9301", "HR-9301", cards.guest("Babic", club), LocalDate.now().minusDays(2),
                LocalDate.now().plusDays(4));
    }

    @Test
    void guestSupplyGoesThroughTheHandlers() throws Exception {
        call(PATH + "/authorize", new AuthorizeRequest("9301", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.guestName").value("Babic"));

        MvcResult started = call(PATH + "/supplyStarted", new SupplyStartedRequest(1, "9301", 3000000, plugIn))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andReturn();
        int transactionId = objectMapper.readTree(started.getResponse().getContentAsString())
                .get("transactionId").asInt();
        assertThat(transactionId).isPositive();

        call(PATH + "/meterReadings", new MeterReadingsRequest(1, transactionId,
                List.of(new MeterReading(plugIn.plusHours(1), 3001200))))
                .andExpect(status().isOk());
        assertThat(socket(1).getMeterKwh()).isEqualByComparingTo("3001.200");

        call(PATH + "/supplyStopped", new SupplyStoppedRequest(transactionId, "9301", 3002000, plugIn.plusHours(3),
                "LOCAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        assertThat(supplySessionDao.findActiveBySocket(socket(1))).isEmpty();
        assertThat(socket(1).getStatus()).isEqualTo(SocketStatus.AVAILABLE);
    }

    @Test
    void cardOfAClubWithoutAgreementIsNotAcceptedHere() throws Exception {
        Group otherClub = groupManager.createChildGroup(console.issuer("HR ISSUERS"), "LINK V3 OTHER CLUB", "Other",
                GroupType.ISSUER);
        cards.card("9302", "HR-9302", cards.guest("Kralj", otherClub), LocalDate.now().minusDays(2),
                LocalDate.now().plusDays(4));

        call(PATH + "/authorize", new AuthorizeRequest("9302", 1))
                .andExpect(jsonPath("$.status").value("NOT_ACCEPTED_HERE"));
    }

    @Test
    void socketStatusWithVendorCodeAndPedestalStatus() throws Exception {
        call(PATH + "/socketStatus", new SocketStatusRequest(2, "FAULTED", "OTHER_ERROR", "ADRIATEC", "E21", null,
                plugIn))
                .andExpect(status().isOk());
        call(PATH + "/socketStatus", new SocketStatusRequest(0, "UNAVAILABLE", "NO_ERROR", "ADRIATEC", null,
                "maintenance", plugIn))
                .andExpect(status().isOk());

        assertThat(socket(2).getStatus()).isEqualTo(SocketStatus.FAULTED);
        assertThat(socketStatusLogDao.findLastBySocketId(socket(2).getId()).getErrorFlags()).isEqualTo(64L);
        assertThat(socket(1).getStatus()).isNotEqualTo(SocketStatus.UNAVAILABLE);

        call(PATH + "/socketStatus", new SocketStatusRequest(1, "SMOKING", null, null, null, null, plugIn))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("WRONG_REQUEST"));
    }

    @Test
    void heartbeatFirmwareAndVendorDataAreAcknowledged() throws Exception {
        mockMvc.perform(post(PATH + "/heartbeat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentTime").exists());
        call(PATH + "/firmwareStatus", new FirmwareStatusRequest("DOWNLOADED"))
                .andExpect(status().isOk());
        call(PATH + "/vendorData", new VendorDataRequest("ADRIATEC", "DOOR_CONTACT", "open"))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        assertThat(pedestalStateDao.findByPedestalId(pedestal.getId())).isNotNull();
    }

    @Test
    void cardListsAndRemoteControlPickTheLinkClientByThePedestalType() {
        assertThat(localCardListSender).isInstanceOf(PedestalLinkClientDispatcher.class);
        assertThat(remoteSupplyControl).isInstanceOf(PedestalLinkClientDispatcher.class);

        // the v3 pedestal of the test has no link address: refused by the v2 client, not by the v1 one
        assertThat(localCardListSender.deleteLocalList(pedestal).errorMessage())
                .isEqualTo("Pedestal has no link address");
        assertThat(remoteSupplyControl.remoteSupplyStart(pedestal, 1, "QR1")).isFalse();
    }

    private Socket socket(int socketNo) {
        return socketDao.findByPedestalCodeAndSocketNo(PEDESTAL, socketNo);
    }
}
