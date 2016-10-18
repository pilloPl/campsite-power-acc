package io.pillopl.webservice.console.issuer;

import io.pillopl.api.IssuerApi;
import io.pillopl.webservice.console.common.AbstractWebService;
import io.pillopl.webservice.console.common.WebServicePaths;
import io.pillopl.webservice.console.common.WsErrorCode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web service of the issuer exchange tab: partners, the sync state matrix, the run of the exchange and a card sent
 * to the partners again.
 */
@RestController
@RequestMapping(WebServicePaths.ISSUER)
public class IssuerWebService extends AbstractWebService<IssuerApi, IssuerWebServiceFault> {

    public IssuerWebService(IssuerApi issuerApi) {
        super(issuerApi);
    }

    @PostMapping("/runIssuerExchange")
    public RunIssuerExchangeResponse runIssuerExchange(@RequestBody RunIssuerExchangeRequest request)
            throws IssuerWebServiceFault {
        return invoke("runIssuerExchange", () -> new RunIssuerExchangeResponse(
                getBean().runIssuerExchange(caller())));
    }

    @PostMapping("/findIssuerPartners")
    public FindIssuerPartnersResponse findIssuerPartners(@RequestBody FindIssuerPartnersRequest request)
            throws IssuerWebServiceFault {
        return invoke("findIssuerPartners", () -> new FindIssuerPartnersResponse(getBean().findIssuerPartners(
                caller())));
    }

    @PostMapping("/saveIssuerPartner")
    public SaveIssuerPartnerResponse saveIssuerPartner(@RequestBody SaveIssuerPartnerRequest request)
            throws IssuerWebServiceFault {
        return invoke("saveIssuerPartner", () -> new SaveIssuerPartnerResponse(getBean().saveIssuerPartner(
                caller(), request.partner())));
    }

    @PostMapping("/findIssuerSyncStatuses")
    public FindIssuerSyncStatusesResponse findIssuerSyncStatuses(@RequestBody FindIssuerSyncStatusesRequest request)
            throws IssuerWebServiceFault {
        return invoke("findIssuerSyncStatuses", () -> new FindIssuerSyncStatusesResponse(
                getBean().findIssuerSyncStatuses(caller())));
    }

    @PostMapping("/resyncCard")
    public ResyncCardResponse resyncCard(@RequestBody ResyncCardRequest request) throws IssuerWebServiceFault {
        return invoke("resyncCard", () -> new ResyncCardResponse(getBean().resyncCard(caller(), request.cardId())));
    }

    @Override
    protected IssuerWebServiceFault createFault(String message, WsErrorCode errorCode) {
        return new IssuerWebServiceFault(message, errorCode);
    }
}
