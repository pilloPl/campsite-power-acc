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
 * Web service of the exchange with the issuer partners.
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

    @Override
    protected IssuerWebServiceFault createFault(String message, WsErrorCode errorCode) {
        return new IssuerWebServiceFault(message, errorCode);
    }
}
