package io.pillopl.webservice.console.qrpay;

import io.pillopl.api.QrPayApi;
import io.pillopl.webservice.console.common.AbstractWebService;
import io.pillopl.webservice.console.common.WebServicePaths;
import io.pillopl.webservice.console.common.WsErrorCode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web service of the payment app: start, stop and follow the QR payment of a socket.
 */
@RestController
@RequestMapping(WebServicePaths.QR_PAY)
public class QrPayWebService extends AbstractWebService<QrPayApi, QrPayWebServiceFault> {

    public QrPayWebService(QrPayApi qrPayApi) {
        super(qrPayApi);
    }

    @PostMapping("/startQrPay")
    public QrPayProcessResponse startQrPay(@RequestBody StartQrPayRequest request) throws QrPayWebServiceFault {
        return invoke("startQrPay", () -> new QrPayProcessResponse(getBean().startQrPay(
                caller(), request.orderReference(), request.socketCode(),
                request.appUserId())));
    }

    @PostMapping("/stopQrPay")
    public QrPayProcessResponse stopQrPay(@RequestBody QrPayProcessRequest request) throws QrPayWebServiceFault {
        return invoke("stopQrPay", () -> new QrPayProcessResponse(getBean().stopQrPay(
                caller(), request.orderReference(), request.appUserId())));
    }

    @PostMapping("/getQrPayProcess")
    public QrPayProcessResponse getQrPayProcess(@RequestBody QrPayProcessRequest request)
            throws QrPayWebServiceFault {
        return invoke("getQrPayProcess", () -> new QrPayProcessResponse(getBean().getQrPayProcess(
                caller(), request.orderReference(), request.appUserId())));
    }

    @Override
    protected QrPayWebServiceFault createFault(String message, WsErrorCode errorCode) {
        return new QrPayWebServiceFault(message, errorCode);
    }
}
