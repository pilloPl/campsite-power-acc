package io.pillopl.webservice.pedestallink.v1;

import io.pillopl.card.CardFixture;
import io.pillopl.dto.pedestallink.v1.AuthorizeRequest;
import io.pillopl.dto.pedestallink.v1.BootRequest;
import io.pillopl.dto.pedestallink.v1.DiagnosticsStatusRequest;
import io.pillopl.dto.pedestallink.v1.FirmwareStatusRequest;
import io.pillopl.dto.pedestallink.v1.MeterReading;
import io.pillopl.dto.pedestallink.v1.MeterReadingsRequest;
import io.pillopl.dto.pedestallink.v1.SocketStatusRequest;
import io.pillopl.dto.pedestallink.v1.SupplyStartedRequest;
import io.pillopl.dto.pedestallink.v1.SupplyStoppedRequest;
import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupType;
import io.pillopl.group.attribute.Notification;
import io.pillopl.group.attribute.NotificationService;
import io.pillopl.group.attribute.NotificationType;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.Socket;
import io.pillopl.infrastructure.SocketStatus;
import io.pillopl.monitoring.SocketStatusLog;
import io.pillopl.persistence.dao.PedestalDao;
import io.pillopl.persistence.dao.PedestalStateDao;
import io.pillopl.persistence.dao.SocketDao;
import io.pillopl.persistence.dao.SocketStatusLogDao;
import io.pillopl.persistence.dao.SupplyIncidentDao;
import io.pillopl.persistence.dao.SupplySessionDao;
import io.pillopl.supply.StopReason;
import io.pillopl.supply.SupplyIncident;
import io.pillopl.supply.SupplySession;
import io.pillopl.webservice.console.WebServiceIntegrationTest;
import io.pillopl.webservice.console.common.WebServicePaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
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
 * A pedestal controller talking link v1 to the pedestal backend: authorize, start, readings, stop, status, boot.
 */
class PedestalLinkV1WebServiceIntegrationTest extends WebServiceIntegrationTest {

    static final String PEDESTAL = "HR-BAY-P201";
    static final String PATH = WebServicePaths.PEDESTAL_LINK_V1 + "/" + PEDESTAL;

    @Autowired GroupManager groupManager;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired CardFixture cards;
    @Autowired PedestalDao pedestalDao;
    @Autowired PedestalStateDao pedestalStateDao;
    @Autowired SocketDao socketDao;
    @Autowired SupplySessionDao supplySessionDao;
    @Autowired SupplyIncidentDao supplyIncidentDao;
    @Autowired SocketStatusLogDao socketStatusLogDao;
    @Autowired NotificationService notificationService;
    @Autowired JdbcTemplate jdbc;

    final LocalDateTime plugIn = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).minusHours(3);
    Pedestal pedestal;
    Group bay, club;

    @BeforeEach
    void setUp() {
        bay = groupManager.createChildGroup(console.site("HR SITES"), "BAY LINK", "Bay", GroupType.SITE);
        club = groupManager.createChildGroup(console.issuer("HR ISSUERS"), "LINK CLUB", "Club",
                GroupType.ISSUER);
        groupManager.addAcceptanceAgreement(club, bay);
        pedestal = infrastructure.pedestal(PEDESTAL, bay, 2);
        pedestal.setBillable(true);
        cards.card("9101", "HR-9101", cards.guest("Kovac", club), LocalDate.now().minusDays(2),
                LocalDate.now().plusDays(4));
    }

    @Test
    void guestAuthorizesStartsReadsAndStopsASupply() throws Exception {
        call(PATH + "/authorize", new AuthorizeRequest("9101", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.guestName").value("Kovac"))
                .andExpect(jsonPath("$.expiryDate").value(LocalDate.now().plusDays(4).toString()));

        MvcResult started = call(PATH + "/supplyStarted", new SupplyStartedRequest(1, "9101", 1250500, plugIn))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.maxCurrentA").value(16))
                .andExpect(jsonPath("$.greeting").value("Kovac"))
                .andReturn();
        Integer transactionId = objectMapper.readTree(started.getResponse().getContentAsString())
                .get("transactionId").asInt();
        assertThat(transactionId).isPositive();

        call(PATH + "/meterReadings", new MeterReadingsRequest(1, transactionId,
                List.of(new MeterReading(plugIn.plusHours(1), 1252000), new MeterReading(plugIn.plusHours(2), -5))))
                .andExpect(status().isOk());
        assertThat(socket(1).getMeterKwh()).isEqualByComparingTo("1252.000");

        call(PATH + "/supplyStopped", new SupplyStoppedRequest(transactionId, "9101", 1254250,
                plugIn.plusHours(3), "UNPLUGGED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        SupplySession session = supplySessionDao.findAll().stream()
                .filter(s -> s.getTransactionId().equals(transactionId)).findFirst().orElseThrow();
        assertThat(session.isActive()).isFalse();
        assertThat(session.getStopReason()).isEqualTo(StopReason.UNPLUGGED);
        assertThat(socket(1).getMeterKwh()).isEqualByComparingTo("1254.250");
        assertThat(socket(1).getStatus()).isEqualTo(SocketStatus.AVAILABLE);
        assertThat(pedestalStateDao.findByPedestalId(pedestal.getId())).isNotNull();
    }

    @Test
    void secondGuestAtAnOccupiedSocketIsToldItIsOccupied() throws Exception {
        cards.card("9102", "HR-9102", cards.guest("Novak", club), LocalDate.now().minusDays(2),
                LocalDate.now().plusDays(4));
        call(PATH + "/supplyStarted", new SupplyStartedRequest(1, "9101", 1250500, plugIn)).andExpect(status().isOk());

        call(PATH + "/authorize", new AuthorizeRequest("9102", 1))
                .andExpect(jsonPath("$.status").value("OCCUPIED"));
    }

    @Test
    void refusedStartGetsTransactionIdZero() throws Exception {
        call(PATH + "/supplyStarted", new SupplyStartedRequest(1, "0000", 1250500, plugIn))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(0))
                .andExpect(jsonPath("$.status").value("INVALID"));
        assertThat(supplySessionDao.findActiveBySocket(socket(1))).isEmpty();
    }

    @Test
    void stopOfAnUnknownTransactionIsAcceptedAndBecomesAnIncident() throws Exception {
        call(PATH + "/supplyStopped", new SupplyStoppedRequest(987654, "9101", 1254250, plugIn, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        assertThat(supplyIncidentDao.findByPedestalCodeOrderByCreateDateDesc(PEDESTAL))
                .extracting(SupplyIncident::getIncidentType)
                .containsExactly(SupplyIncident.IncidentType.NO_SESSION);
    }

    @Test
    void socketStatusIsStoredAndAnUnknownStatusIsAWrongRequest() throws Exception {
        call(PATH + "/socketStatus", new SocketStatusRequest(2, "FAULTED", "GROUND_FAILURE", null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentTime").exists());
        assertThat(socket(2).getStatus()).isEqualTo(SocketStatus.FAULTED);

        call(PATH + "/socketStatus", new SocketStatusRequest(2, "SMOKING", null, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("WRONG_REQUEST"));
    }

    @Test
    void messageForAnUnknownSocketIsAWrongRequest() throws Exception {
        call(PATH + "/supplyStarted", new SupplyStartedRequest(5, "9101", 1250500, plugIn))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bootOfAKnownPedestalIsAcceptedWithTheHeartbeatIntervalAndStoresItsFirmware() throws Exception {
        call(PATH + "/boot", new BootRequest("ACME", "P4", "SN-77", "4.2.1", "8938500000000000001", "MID", "M-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.heartbeatInterval").value(600));

        Pedestal booted = pedestalDao.findByCode(PEDESTAL);
        assertThat(booted.getFirmwareVersion()).isEqualTo("4.2.1");
        assertThat(booted.getSerialNumber()).isEqualTo("SN-77");

        call(WebServicePaths.PEDESTAL_LINK_V1 + "/HR-NOWHERE-P9/boot", new BootRequest("ACME", "P4", null, null,
                null, null, null))
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void heartbeatIsAnsweredWithTheTimeOfThePedestalBackend() throws Exception {
        mockMvc.perform(post(PATH + "/heartbeat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentTime").exists());
        assertThat(pedestalStateDao.findByPedestalId(pedestal.getId())).isNotNull();
    }

    @Test
    void trippedBreakerIsMonitoredAndMailedOnceToTheServiceOfTheCampsite() throws Exception {
        serviceRecipient("service@bay.test", NotificationType.PEDESTAL_STATUS);

        call(PATH + "/socketStatus", new SocketStatusRequest(1, "FAULTED", "BREAKER_TRIPPED", null))
                .andExpect(status().isOk());
        call(PATH + "/socketStatus", new SocketStatusRequest(1, "FAULTED", "BREAKER_TRIPPED", null))
                .andExpect(status().isOk());

        assertThat(socket(1).getStatus()).isEqualTo(SocketStatus.BREAKER_TRIPPED);
        SocketStatusLog log = socketStatusLogDao.findLastBySocketId(socket(1).getId());
        assertThat(log.getStatus()).isEqualTo(SocketStatus.BREAKER_TRIPPED);
        assertThat(log.getErrorFlags()).isEqualTo(1L);
        assertThat(mailSubjectsTo("service@bay.test"))
                .containsExactly("Socket HR-BAY-P201_1 of pedestal HR-BAY-P201: BRK");
    }

    @Test
    void meterReadingsMoveTheMonitoringLogOfASupplyingSocket() throws Exception {
        call(PATH + "/socketStatus", new SocketStatusRequest(2, "OCCUPIED", "NO_ERROR", null))
                .andExpect(status().isOk());
        call(PATH + "/meterReadings", new MeterReadingsRequest(2, null, List.of(
                new MeterReading(plugIn.plusMinutes(5), 1251000), new MeterReading(plugIn.plusMinutes(6), -5))))
                .andExpect(status().isOk());

        assertThat(socket(2).getMeterKwh()).isEqualByComparingTo("1251.000");
        assertThat(socketStatusLogDao.findLastsBySocketId(socket(2).getId(), Pageable.unpaged()))
                .singleElement().satisfies(log -> {
                    assertThat(log.getStatus()).isEqualTo(SocketStatus.SUPPLYING);
                    assertThat(log.getMeterKwh()).isEqualByComparingTo("1251.000");
                });
    }

    @Test
    void firmwareAndDiagnosticsStatusAreOnlyMailedAndAlwaysAcknowledged() throws Exception {
        serviceRecipient("firmware@bay.test", NotificationType.FIRMWARE_STATUS);

        call(PATH + "/firmwareStatus", new FirmwareStatusRequest("INSTALLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentTime").exists());
        call(PATH + "/diagnosticsStatus", new DiagnosticsStatusRequest("UPLOAD_FAILED"))
                .andExpect(status().isOk());
        call(WebServicePaths.PEDESTAL_LINK_V1 + "/HR-NOWHERE-P9/firmwareStatus",
                new FirmwareStatusRequest("INSTALLED"))
                .andExpect(status().isOk());

        assertThat(mailSubjectsTo("firmware@bay.test"))
                .containsExactly("Firmware update of pedestal HR-BAY-P201: INSTALLED");
    }

    private void serviceRecipient(String email, NotificationType notificationType) {
        Notification notification = new Notification("Bay service", email, notificationType);
        notification.setGroup(bay);
        notificationService.persist(notification);
    }

    private List<String> mailSubjectsTo(String recipient) {
        return jdbc.queryForList("SELECT INFO_FIELD_1 FROM AUDIT WHERE ACTION_TYPE = 'EMAIL' AND ACTION_NAME = ?"
                + " ORDER BY ID", String.class, recipient);
    }

    private Socket socket(int socketNo) {
        return socketDao.findByPedestalCodeAndSocketNo(PEDESTAL, socketNo);
    }
}
