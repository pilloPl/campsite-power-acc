package io.pillopl.webservice.console.qrpayconsole;

import io.pillopl.api.QrPayConsoleApi;
import io.pillopl.commons.DateRange;
import io.pillopl.commons.ListPage;
import io.pillopl.commons.Pagination;
import io.pillopl.dto.qrpay.QrPayProcessOverviewDTO;
import io.pillopl.webservice.console.common.AbstractWebService;
import io.pillopl.webservice.console.common.WebServicePaths;
import io.pillopl.webservice.console.common.WsErrorCode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web service of the QR payments tab: process list, timeline with the state machine, cancel of a stuck process and
 * the archive run.
 */
@RestController
@RequestMapping(WebServicePaths.QR_PAY_CONSOLE)
public class QrPayConsoleWebService extends AbstractWebService<QrPayConsoleApi, QrPayConsoleWebServiceFault> {

    public QrPayConsoleWebService(QrPayConsoleApi qrPayConsoleApi) {
        super(qrPayConsoleApi);
    }

    @PostMapping("/findQrPayProcesses")
    public FindQrPayProcessesResponse findQrPayProcesses(@RequestBody FindQrPayProcessesRequest request)
            throws QrPayConsoleWebServiceFault {
        return invoke("findQrPayProcesses", () -> {
            ListPage<QrPayProcessOverviewDTO> page = getBean().findQrPayProcesses(caller(), request.state(),
                    request.campsiteGroupId(), DateRange.ofDays(request.from(), request.to()),
                    Pagination.orFirstPage(request.page(), request.rowsPerPage()));
            return new FindQrPayProcessesResponse(page.items(), page.total(), page.page(), page.rowsPerPage());
        });
    }

    @PostMapping("/getQrPayTimeline")
    public QrPayTimelineResponse getQrPayTimeline(@RequestBody QrPayProcessIdRequest request)
            throws QrPayConsoleWebServiceFault {
        return invoke("getQrPayTimeline", () -> new QrPayTimelineResponse(getBean().getQrPayTimeline(caller(),
                request.processId())));
    }

    @PostMapping("/cancelQrPayProcess")
    public QrPayTimelineResponse cancelQrPayProcess(@RequestBody QrPayProcessIdRequest request)
            throws QrPayConsoleWebServiceFault {
        return invoke("cancelQrPayProcess", () -> new QrPayTimelineResponse(getBean().cancelQrPayProcess(caller(),
                request.processId())));
    }

    @PostMapping("/runQrPayArchive")
    public RunQrPayArchiveResponse runQrPayArchive(@RequestBody RunQrPayArchiveRequest request)
            throws QrPayConsoleWebServiceFault {
        return invoke("runQrPayArchive", () -> new RunQrPayArchiveResponse(getBean().runQrPayArchive(caller())));
    }

    @Override
    protected QrPayConsoleWebServiceFault createFault(String message, WsErrorCode errorCode) {
        return new QrPayConsoleWebServiceFault(message, errorCode);
    }
}
