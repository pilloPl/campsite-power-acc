package io.pillopl.webservice.console.qrpayconsole;

import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupType;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.Socket;
import io.pillopl.pedestallink.v1.PedestalLinkClientV1;
import io.pillopl.persistence.dao.SocketDao;
import io.pillopl.user.UserroleName;
import io.pillopl.webservice.console.WebServiceIntegrationTest;
import io.pillopl.webservice.console.common.WebServicePaths;
import io.pillopl.webservice.console.qrpay.QrPayProcessRequest;
import io.pillopl.webservice.console.qrpay.StartQrPayRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QrPayConsoleWebServiceIntegrationTest extends WebServiceIntegrationTest {

    static final String PATH = WebServicePaths.QR_PAY_CONSOLE;
    static final String SOCKET_CODE = "HRBAY*QRPT*1";

    @MockitoBean PedestalLinkClientV1 pedestalLink;

    @Autowired GroupManager groupManager;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired SocketDao socketDao;

    Long processId;

    @BeforeEach
    void setUp() throws Exception {
        Group bay = groupManager.createChildGroup(console.site("HR SITES"), "BAY QR TAB", "Bay", GroupType.SITE);
        Pedestal pedestal = infrastructure.pedestal("HR-BAYQT-P001", bay, 1);
        Socket socket = pedestal.getSockets().get(0);
        socket.setSocketCode(SOCKET_CODE);
        socketDao.saveAndFlush(socket);
        console.user("payapp", UserroleName.QRPAY_APP_CLIENT, console.staff("HR STAFF"));
        console.user("office", UserroleName.BACK_OFFICE, console.staff("HR STAFF"), bay);
        console.user("desk", UserroleName.RECEPTION, console.staff("HR RECEPTION"), bay);
        when(pedestalLink.remoteSupplyStart(any(), anyInt(), anyString())).thenReturn(true);
        String body = call("payapp", WebServicePaths.QR_PAY + "/startQrPay",
                new StartQrPayRequest("TAB-1", SOCKET_CODE, "guest-1")).andReturn().getResponse()
                .getContentAsString();
        processId = objectMapper.readTree(body).path("qrPayProcess").path("processId").asLong();
        call("payapp", WebServicePaths.QR_PAY + "/stopQrPay", new QrPayProcessRequest("TAB-1", "guest-1"));
    }

    @Test
    void backOfficeListsThePaymentsAndSeesTheTimelineWithTheStateMachine() throws Exception {
        call("office", PATH + "/findQrPayProcesses", new FindQrPayProcessesRequest(null, null, null, null, 0, 10))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.processes[0].orderReference").value("TAB-1"))
                .andExpect(jsonPath("$.processes[0].state").value("ERROR"))
                .andExpect(jsonPath("$.processes[0].campsiteName").value("Bay"));

        call("office", PATH + "/getQrPayTimeline", new QrPayProcessIdRequest(processId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeline.currentState").value("ERROR"))
                .andExpect(jsonPath("$.timeline.steps[0].state").value("START_REQUESTED"))
                .andExpect(jsonPath("$.timeline.states", hasSize(11)))
                .andExpect(jsonPath("$.timeline.cancellable").value(false));
    }

    @Test
    void finishedPaymentCannotBeCancelledAndReceptionMayNotSeeTheTab() throws Exception {
        call("office", PATH + "/cancelQrPayProcess", new QrPayProcessIdRequest(processId))
                .andExpect(status().isConflict());
        call("desk", PATH + "/findQrPayProcesses", new FindQrPayProcessesRequest(null, null, null, null, null,
                null)).andExpect(status().isForbidden());
        call("desk", PATH + "/runQrPayArchive", new RunQrPayArchiveRequest()).andExpect(status().isForbidden());
        call("office", PATH + "/runQrPayArchive", new RunQrPayArchiveRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archivedRows").value(0));
        call("office", PATH + "/getQrPayTimeline", new QrPayProcessIdRequest(987_654_321L))
                .andExpect(status().isNotFound());
    }
}
