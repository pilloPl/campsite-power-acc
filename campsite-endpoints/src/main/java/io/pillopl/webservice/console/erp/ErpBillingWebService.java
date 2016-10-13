package io.pillopl.webservice.console.erp;

import io.pillopl.api.ErpBillingApi;
import io.pillopl.api.exception.InvalidInputException;
import io.pillopl.webservice.console.common.AbstractWebService;
import io.pillopl.webservice.console.common.WebServicePaths;
import io.pillopl.webservice.console.common.WsErrorCode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web service of the ERP billing tab: runs per month, their lines, preview and run of a month, retry of refused
 * lines and the billable records no export takes.
 */
@RestController
@RequestMapping(WebServicePaths.ERP_BILLING)
public class ErpBillingWebService extends AbstractWebService<ErpBillingApi, ErpBillingWebServiceFault> {

    public ErpBillingWebService(ErpBillingApi erpBillingApi) {
        super(erpBillingApi);
    }

    @PostMapping("/findErpBillingRuns")
    public FindErpBillingRunsResponse findErpBillingRuns(@RequestBody FindErpBillingRunsRequest request)
            throws ErpBillingWebServiceFault {
        return invoke("findErpBillingRuns", () -> new FindErpBillingRunsResponse(getBean().findErpBillingRuns(
                caller())));
    }

    @PostMapping("/findErpBillingLines")
    public ErpBillingLinesResponse findErpBillingLines(@RequestBody FindErpBillingLinesRequest request)
            throws ErpBillingWebServiceFault {
        return invoke("findErpBillingLines", () -> new ErpBillingLinesResponse(getBean().findErpBillingLines(
                caller(), request.billingPeriod(), request.exportStatus())));
    }

    @PostMapping("/previewErpBilling")
    public ErpBillingLinesResponse previewErpBilling(@RequestBody ErpBillingMonthRequest request)
            throws ErpBillingWebServiceFault {
        return invoke("previewErpBilling", () -> new ErpBillingLinesResponse(getBean().previewErpBilling(caller(),
                required(request.month(), "Month"), required(request.year(), "Year"))));
    }

    @PostMapping("/runErpBilling")
    public ErpBillingLinesResponse runErpBilling(@RequestBody ErpBillingMonthRequest request)
            throws ErpBillingWebServiceFault {
        return invoke("runErpBilling", () -> new ErpBillingLinesResponse(getBean().runErpBilling(caller(),
                required(request.month(), "Month"), required(request.year(), "Year"))));
    }

    @PostMapping("/retryErpExport")
    public RetryErpExportResponse retryErpExport(@RequestBody RetryErpExportRequest request)
            throws ErpBillingWebServiceFault {
        return invoke("retryErpExport", () -> new RetryErpExportResponse(getBean().retryErpExport(caller(),
                request.billingPeriod())));
    }

    @PostMapping("/findRecordsWithoutExport")
    public FindRecordsWithoutExportResponse findRecordsWithoutExport(
            @RequestBody FindRecordsWithoutExportRequest request) throws ErpBillingWebServiceFault {
        return invoke("findRecordsWithoutExport", () -> new FindRecordsWithoutExportResponse(
                getBean().findRecordsWithoutExport(caller())));
    }

    private static int required(Integer value, String what) {
        if (value == null) {
            throw new InvalidInputException(what + " is missing");
        }
        return value;
    }

    @Override
    protected ErpBillingWebServiceFault createFault(String message, WsErrorCode errorCode) {
        return new ErpBillingWebServiceFault(message, errorCode);
    }
}
