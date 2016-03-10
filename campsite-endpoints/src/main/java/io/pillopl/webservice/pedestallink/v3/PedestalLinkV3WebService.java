package io.pillopl.webservice.pedestallink.v3;

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
import io.pillopl.pedestallink.v3.PedestalLinkServiceV3;
import io.pillopl.pedestallink.v3.PedestalLinkV3Handler;
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
 * Endpoint of the pedestal link v3: {@code <link v3 path>/<pedestal code>/<message>}; every message is handed to its
 * handler in {@link PedestalLinkServiceV3}.
 */
@RestController
@RequestMapping(WebServicePaths.PEDESTAL_LINK_V3 + "/{pedestalCode}")
public class PedestalLinkV3WebService extends AbstractWebService<PedestalLinkServiceV3, PedestalLinkWebServiceFault> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PedestalLinkV3WebService.class);

    public PedestalLinkV3WebService(PedestalLinkServiceV3 pedestalLinkServiceV3) {
        super(pedestalLinkServiceV3);
    }

    @PostMapping("/authorize")
    public AuthorizeResponse authorize(@PathVariable String pedestalCode, @RequestBody AuthorizeRequest request)
            throws PedestalLinkWebServiceFault {
        LOGGER.debug(String.format("Link v3 call authorize(%s, %s)", pedestalCode, request));
        return invoke("authorize", () -> getBean().handleRequest(PedestalLinkV3Handler.Authorize.class,
                pedestalCode, request));
    }

    @PostMapping("/boot")
    public BootResponse boot(@PathVariable String pedestalCode, @RequestBody BootRequest request)
            throws PedestalLinkWebServiceFault {
        LOGGER.debug(String.format("Link v3 call boot(%s, %s)", pedestalCode, request));
        return invoke("boot", () -> getBean().handleRequest(PedestalLinkV3Handler.Boot.class, pedestalCode,
                request));
    }

    @PostMapping("/heartbeat")
    public AcknowledgeResponse heartbeat(@PathVariable String pedestalCode) throws PedestalLinkWebServiceFault {
        LOGGER.debug(String.format("Link v3 call heartbeat(%s)", pedestalCode));
        return invoke("heartbeat", () -> getBean().handleRequest(PedestalLinkV3Handler.Heartbeat.class,
                pedestalCode, null));
    }

    @PostMapping("/supplyStarted")
    public SupplyStartedResponse supplyStarted(@PathVariable String pedestalCode,
                                               @RequestBody SupplyStartedRequest request)
            throws PedestalLinkWebServiceFault {
        LOGGER.debug(String.format("Link v3 call supplyStarted(%s, %s)", pedestalCode, request));
        return invoke("supplyStarted", () -> getBean().handleRequest(PedestalLinkV3Handler.SupplyStarted.class,
                pedestalCode, request));
    }

    @PostMapping("/supplyStopped")
    public SupplyStoppedResponse supplyStopped(@PathVariable String pedestalCode,
                                               @RequestBody SupplyStoppedRequest request)
            throws PedestalLinkWebServiceFault {
        LOGGER.debug(String.format("Link v3 call supplyStopped(%s, %s)", pedestalCode, request));
        return invoke("supplyStopped", () -> getBean().handleRequest(PedestalLinkV3Handler.SupplyStopped.class,
                pedestalCode, request));
    }

    @PostMapping("/socketStatus")
    public AcknowledgeResponse socketStatus(@PathVariable String pedestalCode,
                                            @RequestBody SocketStatusRequest request)
            throws PedestalLinkWebServiceFault {
        LOGGER.debug(String.format("Link v3 call socketStatus(%s, %s)", pedestalCode, request));
        return invoke("socketStatus", () -> getBean().handleRequest(
                PedestalLinkV3Handler.SocketStatusHandler.class, pedestalCode, request));
    }

    @PostMapping("/meterReadings")
    public AcknowledgeResponse meterReadings(@PathVariable String pedestalCode,
                                             @RequestBody MeterReadingsRequest request)
            throws PedestalLinkWebServiceFault {
        LOGGER.debug(String.format("Link v3 call meterReadings(%s, %s)", pedestalCode, request));
        return invoke("meterReadings", () -> getBean().handleRequest(PedestalLinkV3Handler.MeterReadings.class,
                pedestalCode, request));
    }

    @PostMapping("/firmwareStatus")
    public AcknowledgeResponse firmwareStatus(@PathVariable String pedestalCode,
                                              @RequestBody FirmwareStatusRequest request)
            throws PedestalLinkWebServiceFault {
        LOGGER.debug(String.format("Link v3 call firmwareStatus(%s, %s)", pedestalCode, request));
        return invoke("firmwareStatus", () -> getBean().handleRequest(PedestalLinkV3Handler.FirmwareStatus.class,
                pedestalCode, request));
    }

    @PostMapping("/diagnosticsStatus")
    public AcknowledgeResponse diagnosticsStatus(@PathVariable String pedestalCode,
                                                 @RequestBody DiagnosticsStatusRequest request)
            throws PedestalLinkWebServiceFault {
        LOGGER.debug(String.format("Link v3 call diagnosticsStatus(%s, %s)", pedestalCode, request));
        return invoke("diagnosticsStatus", () -> getBean().handleRequest(
                PedestalLinkV3Handler.DiagnosticsStatus.class, pedestalCode, request));
    }

    @PostMapping("/vendorData")
    public VendorDataResponse vendorData(@PathVariable String pedestalCode, @RequestBody VendorDataRequest request)
            throws PedestalLinkWebServiceFault {
        LOGGER.debug(String.format("Link v3 call vendorData(%s, %s)", pedestalCode, request));
        return invoke("vendorData", () -> getBean().handleRequest(PedestalLinkV3Handler.VendorData.class,
                pedestalCode, request));
    }

    @Override
    protected PedestalLinkWebServiceFault createFault(String message, WsErrorCode errorCode) {
        return new PedestalLinkWebServiceFault(message, errorCode);
    }
}
