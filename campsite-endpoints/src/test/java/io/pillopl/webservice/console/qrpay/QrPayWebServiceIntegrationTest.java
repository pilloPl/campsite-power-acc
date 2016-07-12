package io.pillopl.webservice.console.qrpay;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QrPayWebServiceIntegrationTest extends WebServiceIntegrationTest {

    static final String PATH = WebServicePaths.QR_PAY;
    static final String SOCKET_CODE = "HRBAY*QRWS*1";

    @MockitoBean PedestalLinkClientV1 pedestalLink;

    @Autowired GroupManager groupManager;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired SocketDao socketDao;

    @BeforeEach
    void setUp() {
        Group bay = groupManager.createChildGroup(console.site("HR SITES"), "BAY QR CONSOLE", "Bay", GroupType.SITE);
        Pedestal pedestal = infrastructure.pedestal("HR-BAYQR-P001", bay, 1);
        Socket socket = pedestal.getSockets().get(0);
        socket.setSocketCode(SOCKET_CODE);
        socketDao.saveAndFlush(socket);
        console.user("payapp", UserroleName.QRPAY_APP_CLIENT, console.staff("HR STAFF"));
        console.user("reception", UserroleName.RECEPTION, console.staff("HR RECEPTION"), bay);
        when(pedestalLink.remoteSupplyStart(any(), anyInt(), anyString())).thenReturn(true);
    }

    @Test
    void appStartsThePaymentAndTheSocketWaitsForTheGuest() throws Exception {
        call("payapp", PATH + "/startQrPay", new StartQrPayRequest("WS-1", SOCKET_CODE, "guest-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qrPayProcess.state").value("SOCKET_START_WAITING"))
                .andExpect(jsonPath("$.qrPayProcess.socketCode").value(SOCKET_CODE))
                .andExpect(jsonPath("$.qrPayProcess.orderReference").value("WS-1"));

        call("payapp", PATH + "/getQrPayProcess", new QrPayProcessRequest("WS-1", "guest-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qrPayProcess.state").value("SOCKET_START_WAITING"));
    }

    @Test
    void stopBeforeTheSupplyIsAnErrorOfThePayment() throws Exception {
        call("payapp", PATH + "/startQrPay", new StartQrPayRequest("WS-2", SOCKET_CODE, "guest-1"));

        call("payapp", PATH + "/stopQrPay", new QrPayProcessRequest("WS-2", "guest-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qrPayProcess.state").value("ERROR"))
                .andExpect(jsonPath("$.qrPayProcess.errorCode").value("SUPPLY_NOT_STARTED"));
    }

    @Test
    void anotherAppUserMayNotStopThePayment() throws Exception {
        call("payapp", PATH + "/startQrPay", new StartQrPayRequest("WS-3", SOCKET_CODE, "guest-1"));

        call("payapp", PATH + "/stopQrPay", new QrPayProcessRequest("WS-3", "guest-2"))
                .andExpect(status().isForbidden());
    }

    @Test
    void refusedCallsGetTheirFaults() throws Exception {
        call("reception", PATH + "/startQrPay", new StartQrPayRequest("WS-4", SOCKET_CODE, "guest-1"))
                .andExpect(status().isForbidden());
        call("payapp", PATH + "/startQrPay", new StartQrPayRequest("WS-4", "HRBAY*NONE*1", "guest-1"))
                .andExpect(status().isNotFound());
        call("payapp", PATH + "/startQrPay", new StartQrPayRequest("WS-4", "no code", "guest-1"))
                .andExpect(status().isBadRequest());
        call("payapp", PATH + "/startQrPay", new StartQrPayRequest("WS-5", SOCKET_CODE, "guest-1"))
                .andExpect(status().isOk());
        call("payapp", PATH + "/startQrPay", new StartQrPayRequest("WS-5", SOCKET_CODE, "guest-1"))
                .andExpect(status().isConflict());
        call("payapp", PATH + "/getQrPayProcess", new QrPayProcessRequest("WS-NONE", "guest-1"))
                .andExpect(status().isNotFound());
    }
}
