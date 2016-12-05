package io.pillopl.webservice.console.monitoring;

import io.pillopl.api.MonitoringApi;
import io.pillopl.commons.DateRange;
import io.pillopl.commons.DateUtils;
import io.pillopl.webservice.console.common.AbstractWebService;
import io.pillopl.webservice.console.common.WebServicePaths;
import io.pillopl.webservice.console.common.WsErrorCode;
import io.pillopl.webservice.console.supply.PedestalCommandResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Web service of the monitoring and service tab: status report, status and communication logs, heartbeat timeouts,
 * failed events, and the service commands (status request, fault reset, fault mail).
 */
@RestController
@RequestMapping(WebServicePaths.MONITORING)
public class MonitoringWebService extends AbstractWebService<MonitoringApi, MonitoringWebServiceFault> {

    public MonitoringWebService(MonitoringApi monitoringApi) {
        super(monitoringApi);
    }

    @PostMapping("/getStatusReport")
    public GetStatusReportResponse getStatusReport(@RequestBody GetStatusReportRequest request)
            throws MonitoringWebServiceFault {
        return invoke("getStatusReport", () -> new GetStatusReportResponse(getBean().getStatusReport(caller(),
                month(request.month()), request.campsiteGroupId())));
    }

    @PostMapping("/findSocketStatusLogs")
    public FindSocketStatusLogsResponse findSocketStatusLogs(@RequestBody FindMonitoringLogsRequest request)
            throws MonitoringWebServiceFault {
        return invoke("findSocketStatusLogs", () -> new FindSocketStatusLogsResponse(getBean().findSocketStatusLogs(
                caller(), request.pedestalCode(), request.socketId(), DateRange.ofDays(request.from(), request.to()),
                request.maxRows())));
    }

    @PostMapping("/findCommunicationLogs")
    public FindCommunicationLogsResponse findCommunicationLogs(@RequestBody FindMonitoringLogsRequest request)
            throws MonitoringWebServiceFault {
        return invoke("findCommunicationLogs", () -> new FindCommunicationLogsResponse(
                getBean().findCommunicationLogs(caller(), request.pedestalCode(),
                        DateRange.ofDays(request.from(), request.to()), request.maxRows())));
    }

    @PostMapping("/findHeartbeatTimeouts")
    public FindHeartbeatTimeoutsResponse findHeartbeatTimeouts(@RequestBody FindMonitoringLogsRequest request)
            throws MonitoringWebServiceFault {
        return invoke("findHeartbeatTimeouts", () -> new FindHeartbeatTimeoutsResponse(
                getBean().findHeartbeatTimeouts(caller(), request.campsiteGroupId())));
    }

    @PostMapping("/findFailedEvents")
    public FindFailedEventsResponse findFailedEvents(@RequestBody FindMonitoringLogsRequest request)
            throws MonitoringWebServiceFault {
        return invoke("findFailedEvents", () -> new FindFailedEventsResponse(getBean().findFailedEvents(caller(),
                request.maxRows())));
    }

    @PostMapping("/requestStatus")
    public PedestalCommandResponse requestStatus(@RequestBody SocketCommandRequest request)
            throws MonitoringWebServiceFault {
        return invoke("requestStatus", () -> new PedestalCommandResponse(getBean().requestStatus(caller(),
                request.pedestalCode(), request.socketNo())));
    }

    @PostMapping("/resetFault")
    public PedestalCommandResponse resetFault(@RequestBody SocketCommandRequest request)
            throws MonitoringWebServiceFault {
        return invoke("resetFault", () -> new PedestalCommandResponse(getBean().resetFault(caller(),
                request.socketId())));
    }

    @PostMapping("/resendFaultMail")
    public PedestalCommandResponse resendFaultMail(@RequestBody SocketCommandRequest request)
            throws MonitoringWebServiceFault {
        return invoke("resendFaultMail", () -> new PedestalCommandResponse(getBean().resendFaultMail(caller(),
                request.socketId())));
    }

    /**
     * "2026-07" or a day of the month; null for the current month.
     */
    static LocalDate month(String month) {
        if (month == null || month.isBlank()) {
            return null;
        }
        String value = month.trim();
        if (value.length() == 7) {
            value = value + "-01";
        }
        return DateUtils.parseStartOfDay(value).toLocalDate().withDayOfMonth(1);
    }

    @Override
    protected MonitoringWebServiceFault createFault(String message, WsErrorCode errorCode) {
        return new MonitoringWebServiceFault(message, errorCode);
    }
}
