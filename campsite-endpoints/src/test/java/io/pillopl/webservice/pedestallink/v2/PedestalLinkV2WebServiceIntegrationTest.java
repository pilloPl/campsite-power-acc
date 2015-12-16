package io.pillopl.webservice.pedestallink.v2;

import io.pillopl.card.CardFixture;
import io.pillopl.dto.pedestallink.v1.AuthorizeRequest;
import io.pillopl.dto.pedestallink.v1.BootRequest;
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
import io.pillopl.monitoring.SocketStatusLog;
import io.pillopl.persistence.dao.PedestalDao;
import io.pillopl.persistence.dao.PedestalTypeDao;
import io.pillopl.persistence.dao.SocketDao;
import io.pillopl.persistence.dao.SocketStatusLogDao;
import io.pillopl.persistence.dao.SupplySessionDao;
import io.pillopl.supply.SupplySession;
import io.pillopl.webservice.console.WebServiceIntegrationTest;
import io.pillopl.webservice.console.common.WebServicePaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A link v2 controller of an ADRIATEC pedestal: the v1 messages on the v2 path, the v2 socket status with vendor error
 * codes and vendor data.
 */
class PedestalLinkV2WebServiceIntegrationTest extends WebServiceIntegrationTest {

    static final String PEDESTAL = "HR-BAY-P301";
    static final String PATH = WebServicePaths.PEDESTAL_LINK_V2 + "/" + PEDESTAL;

    @Autowired GroupManager groupManager;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired CardFixture cards;
    @Autowired PedestalDao pedestalDao;
    @Autowired PedestalTypeDao pedestalTypeDao;
    @Autowired SocketDao socketDao;
    @Autowired SupplySessionDao supplySessionDao;
    @Autowired SocketStatusLogDao socketStatusLogDao;

    final LocalDateTime plugIn = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).minusHours(3);

    @BeforeEach
    void setUp() {
        Group bay = groupManager.createChildGroup(console.site("HR SITES"), "BAY LINK V2", "Bay", GroupType.SITE);
        Group club = groupManager.createChildGroup(console.issuer("HR ISSUERS"), "LINK V2 CLUB", "Club",
                GroupType.ISSUER);
        groupManager.addAcceptanceAgreement(club, bay);
        PedestalType adriatec = new PedestalType("ADRIATEC", "AT-2", PedestalLinkVersion.V2);
        adriatec.setMaxSockets(4);
        Pedestal pedestal = infrastructure.pedestal(PEDESTAL, bay, 2);
        pedestal.setPedestalType(pedestalTypeDao.saveAndFlush(adriatec));
        pedestal.setBillable(true);
        pedestalDao.saveAndFlush(pedestal);
        cards.card("9201", "HR-9201", cards.guest("Horvat", club), LocalDate.now().minusDays(2),
                LocalDate.now().plusDays(4));
    }

    @Test
    void guestStartsAndStopsASupplyWithTheV1MessagesOnTheV2Path() throws Exception {
        call(PATH + "/authorize", new AuthorizeRequest("9201", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        MvcResult started = call(PATH + "/supplyStarted", new SupplyStartedRequest(1, "9201", 2000000, plugIn))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andReturn();
        int transactionId = objectMapper.readTree(started.getResponse().getContentAsString())
                .get("transactionId").asInt();

        call(PATH + "/supplyStopped", new SupplyStoppedRequest(transactionId, "9201", 2004500, plugIn.plusHours(3),
                "UNPLUGGED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        SupplySession session = supplySessionDao.findAll().stream()
                .filter(s -> s.getTransactionId().equals(transactionId)).findFirst().orElseThrow();
        assertThat(session.isActive()).isFalse();
        assertThat(socket(1).getMeterKwh()).isEqualByComparingTo("2004.500");
    }

    @Test
    void vendorErrorCodeIsTranslatedWithTheMaskOfTheManufacturer() throws Exception {
        call(PATH + "/socketStatus", new SocketStatusRequest(2, "FAULTED", "OTHER_ERROR", "ADRIATEC", "E02",
                "RCD open", plugIn))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentTime").exists());

        assertThat(socket(2).getStatus()).isEqualTo(SocketStatus.FAULTED);
        SocketStatusLog log = socketStatusLogDao.findLastBySocketId(socket(2).getId());
        assertThat(log.getErrorFlags()).isEqualTo(2L);
    }

    @Test
    void trippedBreakerReportedByTheControllerItselfWithoutVendorError() throws Exception {
        call(PATH + "/socketStatus", new SocketStatusRequest(1, "BREAKER_TRIPPED", "BREAKER_TRIPPED", "ADRIATEC",
                "E00", null, plugIn))
                .andExpect(status().isOk());

        assertThat(socket(1).getStatus()).isEqualTo(SocketStatus.BREAKER_TRIPPED);
        assertThat(socketStatusLogDao.findLastBySocketId(socket(1).getId()).getErrorFlags()).isEqualTo(1L);
    }

    @Test
    void pluggedInSocketWithoutSupplyIsOccupied() throws Exception {
        call(PATH + "/socketStatus", new SocketStatusRequest(1, "OCCUPIED", "NO_ERROR", "ADRIATEC", null, null,
                plugIn))
                .andExpect(status().isOk());

        assertThat(socket(1).getStatus()).isEqualTo(SocketStatus.OCCUPIED);
    }

    @Test
    void unknownStatusAndUnknownSocketAreWrongRequests() throws Exception {
        call(PATH + "/socketStatus", new SocketStatusRequest(1, "SMOKING", null, null, null, null, plugIn))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("WRONG_REQUEST"));
        call(PATH + "/socketStatus", new SocketStatusRequest(0, "AVAILABLE", null, null, null, null, plugIn))
                .andExpect(status().isBadRequest());
    }

    @Test
    void vendorDataIsAcceptedOnlyFromTheManufacturer() throws Exception {
        call(PATH + "/vendorData", new VendorDataRequest("ADRIATEC", "HOUSING_TEMPERATURE", "41.5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
        call(PATH + "/vendorData", new VendorDataRequest("ACME", "HOUSING_TEMPERATURE", "41.5"))
                .andExpect(jsonPath("$.status").value("UNKNOWN_VENDOR"));
        call(WebServicePaths.PEDESTAL_LINK_V2 + "/HR-NOWHERE-P9/vendorData",
                new VendorDataRequest("ADRIATEC", "HOUSING_TEMPERATURE", "41.5"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bootAndHeartbeatAreAnsweredLikeOnLinkV1() throws Exception {
        call(PATH + "/boot", new BootRequest("ADRIATEC", "AT-2", "SN-301", "2.0.7", null, "MID", "M-301"))
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.heartbeatInterval").value(600));
        mockMvc.perform(post(PATH + "/heartbeat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentTime").exists());

        assertThat(pedestalDao.findByCode(PEDESTAL).getFirmwareVersion()).isEqualTo("2.0.7");
    }

    private Socket socket(int socketNo) {
        return socketDao.findByPedestalCodeAndSocketNo(PEDESTAL, socketNo);
    }
}
