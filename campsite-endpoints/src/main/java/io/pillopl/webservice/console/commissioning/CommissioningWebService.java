package io.pillopl.webservice.console.commissioning;

import io.pillopl.api.CommissioningApi;
import io.pillopl.value.InstallerToken;
import io.pillopl.value.PedestalCode;
import io.pillopl.value.PedestalId;
import io.pillopl.value.Username;
import io.pillopl.webservice.console.common.AbstractWebService;
import io.pillopl.webservice.console.common.WebServicePaths;
import io.pillopl.webservice.console.common.WsErrorCode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Web service of the pedestal life cycle: supplier deliveries, supplier stock, commissioning and decommissioning.
 */
@RestController
@RequestMapping(WebServicePaths.COMMISSIONING)
public class CommissioningWebService extends AbstractWebService<CommissioningApi, CommissioningWebServiceFault> {

    public CommissioningWebService(CommissioningApi commissioningApi) {
        super(commissioningApi);
    }

    @PostMapping("/findCommissionings")
    public FindCommissioningsResponse findCommissionings(@RequestBody FindCommissioningsRequest request)
            throws CommissioningWebServiceFault {
        return invoke("findCommissionings", () -> new FindCommissioningsResponse(
                getBean().findCommissionings(caller())));
    }

    @PostMapping("/findCommissioningByToken")
    public FindCommissioningByTokenResponse findCommissioningByToken(
            @RequestBody FindCommissioningByTokenRequest request) throws CommissioningWebServiceFault {
        return invoke("findCommissioningByToken", () -> new FindCommissioningByTokenResponse(
                getBean().findCommissioningByToken(caller(),
                        InstallerToken.of(request.token()))));
    }

    @PostMapping("/createCommissioning")
    public CreateCommissioningResponse createCommissioning(@RequestBody CreateCommissioningRequest request)
            throws CommissioningWebServiceFault {
        return invoke("createCommissioning", () -> new CreateCommissioningResponse(getBean().createCommissioning(
                caller(), Username.of(request.ownerUsername()), request.location(),
                request.comments(), Boolean.TRUE.equals(request.listedInApp()), request.accessMode())));
    }

    @PostMapping("/getSupplierStock")
    public GetSupplierStockResponse getSupplierStock(@RequestBody GetSupplierStockRequest request)
            throws CommissioningWebServiceFault {
        return invoke("getSupplierStock", () -> new GetSupplierStockResponse(
                getBean().getSupplierStock(caller())));
    }

    @PostMapping("/commissionPedestals")
    public CommissionPedestalsResponse commissionPedestals(@RequestBody CommissionPedestalsRequest request)
            throws CommissioningWebServiceFault {
        return invoke("commissionPedestals", () -> {
            List<PedestalId> ids = new ArrayList<>();
            for (Long id : request.pedestalIds()) {
                ids.add(PedestalId.of(id));
            }
            return new CommissionPedestalsResponse(getBean().commissionPedestals(
                    caller(), InstallerToken.of(request.token()), ids));
        });
    }

    @PostMapping("/commissionPedestal")
    public CommissionPedestalResponse commissionPedestal(@RequestBody CommissionPedestalRequest request)
            throws CommissioningWebServiceFault {
        return invoke("commissionPedestal", () -> {
            PedestalCode code = PedestalCode.of(request.pedestalCode());
            getBean().commissionPedestal(caller(), code,
                    Username.of(request.ownerUsername()), request.location(),
                    Boolean.TRUE.equals(request.listedInApp()), request.accessMode());
            return new CommissionPedestalResponse(code.value(), true);
        });
    }

    @PostMapping("/decommissionPedestal")
    public DecommissionPedestalResponse decommissionPedestal(@RequestBody DecommissionPedestalRequest request)
            throws CommissioningWebServiceFault {
        return invoke("decommissionPedestal", () -> {
            PedestalId pedestalId = PedestalId.of(request.pedestalId());
            getBean().decommissionPedestal(caller(), pedestalId);
            return new DecommissionPedestalResponse(pedestalId.value(), true);
        });
    }

    @PostMapping("/importSupplierPedestals")
    public ImportSupplierPedestalsResponse importSupplierPedestals(
            @RequestBody ImportSupplierPedestalsRequest request) throws CommissioningWebServiceFault {
        return invoke("importSupplierPedestals", () -> new ImportSupplierPedestalsResponse(
                getBean().importSupplierPedestals(caller(), request.pedestals(),
                        Boolean.TRUE.equals(request.networkOwned()))));
    }

    @Override
    protected CommissioningWebServiceFault createFault(String message, WsErrorCode errorCode) {
        return new CommissioningWebServiceFault(message, errorCode);
    }
}
