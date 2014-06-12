package io.pillopl.webservice.pedestallink.v1;

import io.pillopl.dto.pedestallink.v1.AcknowledgeResponse;
import io.pillopl.dto.pedestallink.v1.AuthorizeRequest;
import io.pillopl.dto.pedestallink.v1.AuthorizeResponse;
import io.pillopl.dto.pedestallink.v1.BootRequest;
import io.pillopl.dto.pedestallink.v1.BootResponse;
import io.pillopl.dto.pedestallink.v1.MeterReadingsRequest;
import io.pillopl.dto.pedestallink.v1.SocketStatusRequest;
import io.pillopl.dto.pedestallink.v1.SupplyStartedRequest;
import io.pillopl.dto.pedestallink.v1.SupplyStartedResponse;
import io.pillopl.dto.pedestallink.v1.SupplyStoppedRequest;
import io.pillopl.dto.pedestallink.v1.SupplyStoppedResponse;
import io.pillopl.pedestallink.v1.PedestalLinkServiceV1;
import io.pillopl.webservice.console.common.AbstractWebService;
import io.pillopl.webservice.console.common.WebServicePaths;
import io.pillopl.webservice.console.common.WsErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint of the pedestal link v1: the controller posts each message to
 * {@code <link v1 path>/<pedestal code>/<message>}; the pedestal code in the path is its identity on the link.
 */
@RestController
@RequestMapping(WebServicePaths.PEDESTAL_LINK_V1 + "/{pedestalCode}")
public class PedestalLinkV1WebService extends AbstractWebService<PedestalLinkServiceV1, PedestalLinkV1WebServiceFault> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PedestalLinkV1WebService.class);

    public PedestalLinkV1WebService(PedestalLinkServiceV1 pedestalLinkServiceV1) {
        super(pedestalLinkServiceV1);
    }

    @PostMapping("/authorize")
    public AuthorizeResponse authorize(@PathVariable String pedestalCode, @RequestBody AuthorizeRequest request)
            throws PedestalLinkV1WebServiceFault {
        LOGGER.debug(String.format("Link v1 call authorize(%s, %s)", pedestalCode, request));
        AuthorizeResponse result = invoke("authorize", () -> getBean().authorize(pedestalCode, request));
        LOGGER.debug(String.format("Link v1 authorize(%s, %s), result=%s", pedestalCode, request, result));
        return result;
    }

    @PostMapping("/boot")
    public BootResponse boot(@PathVariable String pedestalCode, @RequestBody BootRequest request)
            throws PedestalLinkV1WebServiceFault {
        LOGGER.debug(String.format("Link v1 call boot(%s, %s)", pedestalCode, request));
        BootResponse result = invoke("boot", () -> getBean().boot(pedestalCode, request));
        LOGGER.debug(String.format("Link v1 boot(%s, %s), result=%s", pedestalCode, request, result));
        return result;
    }

    @PostMapping("/heartbeat")
    public AcknowledgeResponse heartbeat(@PathVariable String pedestalCode) throws PedestalLinkV1WebServiceFault {
        LOGGER.debug(String.format("Link v1 call heartbeat(%s)", pedestalCode));
        return invoke("heartbeat", () -> getBean().heartbeat(pedestalCode));
    }

    @PostMapping("/supplyStarted")
    public SupplyStartedResponse supplyStarted(@PathVariable String pedestalCode,
                                               @RequestBody SupplyStartedRequest request)
            throws PedestalLinkV1WebServiceFault {
        LOGGER.debug(String.format("Link v1 call supplyStarted(%s, %s)", pedestalCode, request));
        SupplyStartedResponse result = invoke("supplyStarted", () -> getBean().supplyStarted(pedestalCode,
                request));
        LOGGER.debug(String.format("Link v1 supplyStarted(%s, %s), result=%s", pedestalCode, request, result));
        return result;
    }

    @PostMapping("/supplyStopped")
    public SupplyStoppedResponse supplyStopped(@PathVariable String pedestalCode,
                                               @RequestBody SupplyStoppedRequest request)
            throws PedestalLinkV1WebServiceFault {
        LOGGER.debug(String.format("Link v1 call supplyStopped(%s, %s)", pedestalCode, request));
        SupplyStoppedResponse result = invoke("supplyStopped", () -> getBean().supplyStopped(pedestalCode,
                request));
        LOGGER.debug(String.format("Link v1 supplyStopped(%s, %s), result=%s", pedestalCode, request, result));
        return result;
    }

    @PostMapping("/socketStatus")
    public AcknowledgeResponse socketStatus(@PathVariable String pedestalCode,
                                            @RequestBody SocketStatusRequest request)
            throws PedestalLinkV1WebServiceFault {
        LOGGER.debug(String.format("Link v1 call socketStatus(%s, %s)", pedestalCode, request));
        return invoke("socketStatus", () -> getBean().socketStatus(pedestalCode, request));
    }

    @PostMapping("/meterReadings")
    public AcknowledgeResponse meterReadings(@PathVariable String pedestalCode,
                                             @RequestBody MeterReadingsRequest request)
            throws PedestalLinkV1WebServiceFault {
        LOGGER.debug(String.format("Link v1 call meterReadings(%s, %s)", pedestalCode, request));
        return invoke("meterReadings", () -> getBean().meterReadings(pedestalCode, request));
    }

    @Override
    protected PedestalLinkV1WebServiceFault createFault(String message, WsErrorCode errorCode) {
        return new PedestalLinkV1WebServiceFault(message, errorCode);
    }
}
