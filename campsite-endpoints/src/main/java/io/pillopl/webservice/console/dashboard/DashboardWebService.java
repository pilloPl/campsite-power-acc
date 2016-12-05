package io.pillopl.webservice.console.dashboard;

import io.pillopl.api.DashboardApi;
import io.pillopl.webservice.console.common.AbstractWebService;
import io.pillopl.webservice.console.common.WebServicePaths;
import io.pillopl.webservice.console.common.WsErrorCode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web service of the dashboard: key figures, live alerts and their acknowledgement.
 */
@RestController
@RequestMapping(WebServicePaths.DASHBOARD)
public class DashboardWebService extends AbstractWebService<DashboardApi, DashboardWebServiceFault> {

    public DashboardWebService(DashboardApi dashboardApi) {
        super(dashboardApi);
    }

    @PostMapping("/getDashboard")
    public GetDashboardResponse getDashboard(@RequestBody GetDashboardRequest request)
            throws DashboardWebServiceFault {
        return invoke("getDashboard", () -> new GetDashboardResponse(getBean().getDashboard(caller(),
                request.campsiteGroupId())));
    }

    @PostMapping("/findAlerts")
    public FindAlertsResponse findAlerts(@RequestBody FindAlertsRequest request) throws DashboardWebServiceFault {
        return invoke("findAlerts", () -> new FindAlertsResponse(getBean().findAlerts(caller(),
                request.campsiteGroupId(), Boolean.TRUE.equals(request.includeAcknowledged()), request.maxRows())));
    }

    @PostMapping("/acknowledgeAlert")
    public AcknowledgeAlertResponse acknowledgeAlert(@RequestBody AcknowledgeAlertRequest request)
            throws DashboardWebServiceFault {
        return invoke("acknowledgeAlert", () -> new AcknowledgeAlertResponse(getBean().acknowledgeAlert(caller(),
                request.alertKey(), request.comment())));
    }

    @Override
    protected DashboardWebServiceFault createFault(String message, WsErrorCode errorCode) {
        return new DashboardWebServiceFault(message, errorCode);
    }
}
