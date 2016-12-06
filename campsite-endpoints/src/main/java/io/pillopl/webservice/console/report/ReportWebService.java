package io.pillopl.webservice.console.report;

import io.pillopl.api.ReportApi;
import io.pillopl.commons.DateRange;
import io.pillopl.webservice.console.common.AbstractWebService;
import io.pillopl.webservice.console.common.WebServicePaths;
import io.pillopl.webservice.console.common.WsErrorCode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web service of the management reports: one operation, the report type picks the report.
 */
@RestController
@RequestMapping(WebServicePaths.REPORT)
public class ReportWebService extends AbstractWebService<ReportApi, ReportWebServiceFault> {

    public ReportWebService(ReportApi reportApi) {
        super(reportApi);
    }

    @PostMapping("/getReport")
    public GetReportResponse getReport(@RequestBody GetReportRequest request) throws ReportWebServiceFault {
        return invoke("getReport", () -> new GetReportResponse(request.reportType(),
                getBean().getReport(caller(), request.reportType(),
                        DateRange.ofDays(request.from(), request.to()), request.campsiteGroupId(),
                        request.powerSectionId(), request.year(), request.month(), request.timeResolution(),
                        request.pedestalCodes())));
    }

    @PostMapping("/refreshUtilization")
    public RefreshUtilizationResponse refreshUtilization(@RequestBody RefreshUtilizationRequest request)
            throws ReportWebServiceFault {
        return invoke("refreshUtilization", () -> {
            getBean().refreshUtilization(caller());
            return new RefreshUtilizationResponse(true);
        });
    }

    @Override
    protected ReportWebServiceFault createFault(String message, WsErrorCode errorCode) {
        return new ReportWebServiceFault(message, errorCode);
    }
}
