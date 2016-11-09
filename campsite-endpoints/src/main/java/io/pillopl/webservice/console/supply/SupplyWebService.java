package io.pillopl.webservice.console.supply;

import io.pillopl.api.SupplyApi;
import io.pillopl.commons.DateRange;
import io.pillopl.commons.ListPage;
import io.pillopl.commons.Pagination;
import io.pillopl.dto.supply.SupplyIncidentDTO;
import io.pillopl.dto.supply.SupplySessionDTO;
import io.pillopl.webservice.console.common.AbstractWebService;
import io.pillopl.webservice.console.common.WebServicePaths;
import io.pillopl.webservice.console.common.WsErrorCode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web service of the supply sessions tab and of the remote control of the map popup: live and recent sessions,
 * remote supply start and stop, closing a stale session, incidents and their review.
 */
@RestController
@RequestMapping(WebServicePaths.SUPPLY)
public class SupplyWebService extends AbstractWebService<SupplyApi, SupplyWebServiceFault> {

    public SupplyWebService(SupplyApi supplyApi) {
        super(supplyApi);
    }

    @PostMapping("/findSupplySessions")
    public FindSupplySessionsResponse findSupplySessions(@RequestBody FindSupplySessionsRequest request)
            throws SupplyWebServiceFault {
        return invoke("findSupplySessions", () -> {
            ListPage<SupplySessionDTO> page = getBean().findSupplySessions(caller(), request.campsiteGroupId(),
                    request.pedestalCode(), request.socketCode(), Boolean.TRUE.equals(request.activeOnly()),
                    DateRange.ofDays(request.from(), request.to()),
                    Pagination.orFirstPage(request.page(), request.rowsPerPage()));
            return new FindSupplySessionsResponse(page.items(), page.total(), page.page(), page.rowsPerPage());
        });
    }

    @PostMapping("/startSupply")
    public PedestalCommandResponse startSupply(@RequestBody StartSupplyRequest request) throws SupplyWebServiceFault {
        return invoke("startSupply", () -> new PedestalCommandResponse(getBean().startSupply(caller(),
                request.socketId(), request.cardRfid())));
    }

    @PostMapping("/stopSupply")
    public PedestalCommandResponse stopSupply(@RequestBody SupplySessionRequest request)
            throws SupplyWebServiceFault {
        return invoke("stopSupply", () -> new PedestalCommandResponse(getBean().stopSupply(caller(),
                request.sessionId())));
    }

    @PostMapping("/closeStaleSession")
    public CloseStaleSessionResponse closeStaleSession(@RequestBody SupplySessionRequest request)
            throws SupplyWebServiceFault {
        return invoke("closeStaleSession", () -> new CloseStaleSessionResponse(getBean().closeStaleSession(
                caller(), request.sessionId())));
    }

    @PostMapping("/findSupplyIncidents")
    public FindSupplyIncidentsResponse findSupplyIncidents(@RequestBody FindSupplyIncidentsRequest request)
            throws SupplyWebServiceFault {
        return invoke("findSupplyIncidents", () -> {
            ListPage<SupplyIncidentDTO> page = getBean().findSupplyIncidents(caller(), request.campsiteGroupId(),
                    request.pedestalCode(), Boolean.TRUE.equals(request.unreviewedOnly()),
                    DateRange.ofDays(request.from(), request.to()),
                    Pagination.orFirstPage(request.page(), request.rowsPerPage()));
            return new FindSupplyIncidentsResponse(page.items(), page.total(), page.page(), page.rowsPerPage());
        });
    }

    @PostMapping("/reviewSupplyIncident")
    public ReviewSupplyIncidentResponse reviewSupplyIncident(@RequestBody ReviewSupplyIncidentRequest request)
            throws SupplyWebServiceFault {
        return invoke("reviewSupplyIncident", () -> new ReviewSupplyIncidentResponse(getBean().reviewSupplyIncident(
                caller(), request.incidentId(), request.comment())));
    }

    @Override
    protected SupplyWebServiceFault createFault(String message, WsErrorCode errorCode) {
        return new SupplyWebServiceFault(message, errorCode);
    }
}
