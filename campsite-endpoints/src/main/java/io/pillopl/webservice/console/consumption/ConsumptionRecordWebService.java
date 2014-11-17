package io.pillopl.webservice.console.consumption;

import io.pillopl.api.ConsumptionRecordApi;
import io.pillopl.commons.DateRange;
import io.pillopl.commons.Pagination;
import io.pillopl.value.Username;
import io.pillopl.webservice.console.common.AbstractWebService;
import io.pillopl.webservice.console.common.WebServicePaths;
import io.pillopl.webservice.console.common.WsErrorCode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web service of the consumption records: the list of the caller's campsites and guests, and the review queue.
 */
@RestController
@RequestMapping(WebServicePaths.CONSUMPTION_RECORD)
public class ConsumptionRecordWebService
        extends AbstractWebService<ConsumptionRecordApi, ConsumptionRecordWebServiceFault> {

    public ConsumptionRecordWebService(ConsumptionRecordApi consumptionRecordApi) {
        super(consumptionRecordApi);
    }

    @PostMapping("/findConsumptionRecords")
    public FindConsumptionRecordsResponse findConsumptionRecords(@RequestBody FindConsumptionRecordsRequest request)
            throws ConsumptionRecordWebServiceFault {
        return invoke("findConsumptionRecords", () -> new FindConsumptionRecordsResponse(
                getBean().findConsumptionRecords(Username.of(request.authUsername()),
                        DateRange.ofDays(request.from(), request.to()), request.validationState(),
                        Pagination.ofNullable(request.page(), request.rowsPerPage()))));
    }

    @PostMapping("/reviewConsumptionRecord")
    public ReviewConsumptionRecordResponse reviewConsumptionRecord(
            @RequestBody ReviewConsumptionRecordRequest request) throws ConsumptionRecordWebServiceFault {
        return invoke("reviewConsumptionRecord", () -> new ReviewConsumptionRecordResponse(
                getBean().reviewConsumptionRecord(Username.of(request.authUsername()), request.consumptionRecordId(),
                        Boolean.TRUE.equals(request.confirmed()), request.comment())));
    }

    @Override
    protected ConsumptionRecordWebServiceFault createFault(String message, WsErrorCode errorCode) {
        return new ConsumptionRecordWebServiceFault(message, errorCode);
    }
}
