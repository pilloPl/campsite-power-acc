package io.pillopl.webservice.pedestallink.v2;

import io.pillopl.dto.pedestallink.v1.AcknowledgeResponse;
import io.pillopl.dto.pedestallink.v1.AuthorizeRequest;
import io.pillopl.dto.pedestallink.v1.AuthorizeResponse;
import io.pillopl.dto.pedestallink.v1.BootRequest;
import io.pillopl.dto.pedestallink.v1.BootResponse;
import io.pillopl.dto.pedestallink.v1.DiagnosticsStatusRequest;
import io.pillopl.dto.pedestallink.v1.FirmwareStatusRequest;
import io.pillopl.dto.pedestallink.v1.MeterReadingsRequest;
import io.pillopl.dto.pedestallink.v1.SupplyStartedRequest;
import io.pillopl.dto.pedestallink.v1.SupplyStartedResponse;
import io.pillopl.dto.pedestallink.v1.SupplyStoppedRequest;
import io.pillopl.dto.pedestallink.v1.SupplyStoppedResponse;
import io.pillopl.dto.pedestallink.v2.SocketStatusRequest;
import io.pillopl.dto.pedestallink.v2.VendorDataRequest;
import io.pillopl.dto.pedestallink.v2.VendorDataResponse;
import io.pillopl.pedestallink.v2.PedestalLinkServiceV2;
import io.pillopl.webservice.pedestallink.PedestalLinkWebServiceFault;
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
 * Endpoint of the pedestal link v2, a copy of the v1 endpoint: the controller posts each message to
 * {@code <link v2 path>/<pedestal code>/<message>}. The v1 messages keep their bodies; socket status has the v2 body
 * and vendor data is new.
 */
@RestController
@RequestMapping(WebServicePaths.PEDESTAL_LINK_V2 + "/{pedestalCode}")
public class PedestalLinkV2WebService extends AbstractWebService<PedestalLinkServiceV2, PedestalLinkWebServiceFault> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PedestalLinkV2WebService.class);

    public PedestalLinkV2WebService(PedestalLinkServiceV2 pedestalLinkServiceV2) {
        super(pedestalLinkServiceV2);
    }

    @PostMapping("/authorize")
    public AuthorizeResponse authorize(@PathVariable String pedestalCode, @RequestBody AuthorizeRequest request)
            throws PedestalLinkWebServiceFault {
        LOGGER.debug(String.format("Link v2 call authorize(%s, %s)", pedestalCode, request));
        AuthorizeResponse result = invoke("authorize", () -> getBean().authorize(pedestalCode, request));
        LOGGER.debug(String.format("Link v2 authorize(%s, %s), result=%s", pedestalCode, request, result));
        return result;
    }

    @PostMapping("/boot")
    public BootResponse boot(@PathVariable String pedestalCode, @RequestBody BootRequest request)
            throws PedestalLinkWebServiceFault {
        LOGGER.debug(String.format("Link v2 call boot(%s, %s)", pedestalCode, request));
        BootResponse result = invoke("boot", () -> getBean().boot(pedestalCode, request));
        LOGGER.debug(String.format("Link v2 boot(%s, %s), result=%s", pedestalCode, request, result));
        return result;
    }

    @PostMapping("/heartbeat")
    public AcknowledgeResponse heartbeat(@PathVariable String pedestalCode) throws PedestalLinkWebServiceFault {
        LOGGER.debug(String.format("Link v2 call heartbeat(%s)", pedestalCode));
        return invoke("heartbeat", () -> getBean().heartbeat(pedestalCode));
    }

    @PostMapping("/supplyStarted")
    public SupplyStartedResponse supplyStarted(@PathVariable String pedestalCode,
                                               @RequestBody SupplyStartedRequest request)
            throws PedestalLinkWebServiceFault {
        LOGGER.debug(String.format("Link v2 call supplyStarted(%s, %s)", pedestalCode, request));
        SupplyStartedResponse result = invoke("supplyStarted", () -> getBean().supplyStarted(pedestalCode,
                request));
        LOGGER.debug(String.format("Link v2 supplyStarted(%s, %s), result=%s", pedestalCode, request, result));
        return result;
    }

    @PostMapping("/supplyStopped")
    public SupplyStoppedResponse supplyStopped(@PathVariable String pedestalCode,
                                               @RequestBody SupplyStoppedRequest request)
            throws PedestalLinkWebServiceFault {
        LOGGER.debug(String.format("Link v2 call supplyStopped(%s, %s)", pedestalCode, request));
        SupplyStoppedResponse result = invoke("supplyStopped", () -> getBean().supplyStopped(pedestalCode,
                request));
        LOGGER.debug(String.format("Link v2 supplyStopped(%s, %s), result=%s", pedestalCode, request, result));
        return result;
    }

    @PostMapping("/socketStatus")
    public AcknowledgeResponse socketStatus(@PathVariable String pedestalCode,
                                            @RequestBody SocketStatusRequest request)
            throws PedestalLinkWebServiceFault {
        LOGGER.debug(String.format("Link v2 call socketStatus(%s, %s)", pedestalCode, request));
        return invoke("socketStatus", () -> getBean().socketStatus(pedestalCode, request));
    }

    @PostMapping("/meterReadings")
    public AcknowledgeResponse meterReadings(@PathVariable String pedestalCode,
                                             @RequestBody MeterReadingsRequest request)
            throws PedestalLinkWebServiceFault {
        LOGGER.debug(String.format("Link v2 call meterReadings(%s, %s)", pedestalCode, request));
        return invoke("meterReadings", () -> getBean().meterReadings(pedestalCode, request));
    }

    @PostMapping("/firmwareStatus")
    public AcknowledgeResponse firmwareStatus(@PathVariable String pedestalCode,
                                              @RequestBody FirmwareStatusRequest request)
            throws PedestalLinkWebServiceFault {
        LOGGER.debug(String.format("Link v2 call firmwareStatus(%s, %s)", pedestalCode, request));
        return invoke("firmwareStatus", () -> getBean().firmwareStatus(pedestalCode, request));
    }

    @PostMapping("/diagnosticsStatus")
    public AcknowledgeResponse diagnosticsStatus(@PathVariable String pedestalCode,
                                                 @RequestBody DiagnosticsStatusRequest request)
            throws PedestalLinkWebServiceFault {
        LOGGER.debug(String.format("Link v2 call diagnosticsStatus(%s, %s)", pedestalCode, request));
        return invoke("diagnosticsStatus", () -> getBean().diagnosticsStatus(pedestalCode, request));
    }

    @PostMapping("/vendorData")
    public VendorDataResponse vendorData(@PathVariable String pedestalCode, @RequestBody VendorDataRequest request)
            throws PedestalLinkWebServiceFault {
        LOGGER.debug(String.format("Link v2 call vendorData(%s, %s)", pedestalCode, request));
        VendorDataResponse result = invoke("vendorData", () -> getBean().vendorData(pedestalCode, request));
        LOGGER.debug(String.format("Link v2 vendorData(%s, %s), result=%s", pedestalCode, request, result));
        return result;
    }

    @Override
    protected PedestalLinkWebServiceFault createFault(String message, WsErrorCode errorCode) {
        return new PedestalLinkWebServiceFault(message, errorCode);
    }
}
