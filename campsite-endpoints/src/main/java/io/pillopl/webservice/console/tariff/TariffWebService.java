package io.pillopl.webservice.console.tariff;

import io.pillopl.api.TariffApi;
import io.pillopl.dto.pricing.TariffApplicabilityDTO;
import io.pillopl.value.GroupId;
import io.pillopl.value.Kwh;
import io.pillopl.value.PedestalCode;
import io.pillopl.value.SocketId;
import io.pillopl.value.TariffId;
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
 * Web service of the tariffs tab: tariffs of the market, assignments to sockets, pedestals and campsites, the
 * tariff of a socket and the price simulation.
 */
@RestController
@RequestMapping(WebServicePaths.TARIFF)
public class TariffWebService extends AbstractWebService<TariffApi, TariffWebServiceFault> {

    public TariffWebService(TariffApi tariffApi) {
        super(tariffApi);
    }

    @PostMapping("/findTariffs")
    public FindTariffsResponse findTariffs(@RequestBody FindTariffsRequest request) throws TariffWebServiceFault {
        return invoke("findTariffs", () -> new FindTariffsResponse(getBean().findTariffs(
                caller())));
    }

    @PostMapping("/getTariff")
    public GetTariffResponse getTariff(@RequestBody GetTariffRequest request) throws TariffWebServiceFault {
        return invoke("getTariff", () -> new GetTariffResponse(getBean().getTariff(
                caller(), TariffId.of(request.tariffId()))));
    }

    @PostMapping("/getDefaultTariff")
    public GetDefaultTariffResponse getDefaultTariff(@RequestBody GetDefaultTariffRequest request)
            throws TariffWebServiceFault {
        return invoke("getDefaultTariff", () -> new GetDefaultTariffResponse(getBean().getDefaultTariff(
                caller())));
    }

    @PostMapping("/existsTariff")
    public ExistsTariffResponse existsTariff(@RequestBody ExistsTariffRequest request) throws TariffWebServiceFault {
        return invoke("existsTariff", () -> new ExistsTariffResponse(request.tariffName(), getBean().existsTariff(
                caller(), request.tariffName())));
    }

    @PostMapping("/saveTariff")
    public SaveTariffResponse saveTariff(@RequestBody SaveTariffRequest request) throws TariffWebServiceFault {
        return invoke("saveTariff", () -> new SaveTariffResponse(getBean().saveTariff(
                caller(), request.tariff())));
    }

    @PostMapping("/updateTariff")
    public UpdateTariffResponse updateTariff(@RequestBody UpdateTariffRequest request) throws TariffWebServiceFault {
        return invoke("updateTariff", () -> new UpdateTariffResponse(getBean().updateTariff(
                caller(), TariffId.of(request.tariffId()), request.tariff())));
    }

    @PostMapping("/setMarketDefaultTariff")
    public SetMarketDefaultTariffResponse setMarketDefaultTariff(@RequestBody SetMarketDefaultTariffRequest request)
            throws TariffWebServiceFault {
        return invoke("setMarketDefaultTariff", () -> new SetMarketDefaultTariffResponse(
                getBean().setMarketDefaultTariff(caller(),
                        TariffId.of(request.tariffId()))));
    }

    @PostMapping("/applyTariffToPedestals")
    public ApplyTariffToPedestalsResponse applyTariffToPedestals(@RequestBody ApplyTariffToPedestalsRequest request)
            throws TariffWebServiceFault {
        return invoke("applyTariffToPedestals", () -> {
            List<PedestalCode> codes = new ArrayList<>();
            if (request.pedestalCodes() != null) {
                for (String code : request.pedestalCodes()) {
                    codes.add(PedestalCode.of(code));
                }
            }
            return new ApplyTariffToPedestalsResponse(getBean().applyTariffToPedestals(
                    caller(), codes, TariffId.ofNullable(request.tariffId()),
                    request.startDate()));
        });
    }

    @PostMapping("/applyTariffToSocket")
    public ApplyTariffToSocketResponse applyTariffToSocket(@RequestBody ApplyTariffToSocketRequest request)
            throws TariffWebServiceFault {
        return invoke("applyTariffToSocket", () -> {
            SocketId socketId = SocketId.of(request.socketId());
            TariffApplicabilityDTO assignment = getBean().applyTariffToSocket(caller(),
                    socketId, TariffId.ofNullable(request.tariffId()), request.startDate());
            return new ApplyTariffToSocketResponse(socketId.value(), assignment, assignment == null);
        });
    }

    @PostMapping("/applyTariffToAffiliatePedestals")
    public ApplyTariffToAffiliatePedestalsResponse applyTariffToAffiliatePedestals(
            @RequestBody ApplyTariffToAffiliatePedestalsRequest request) throws TariffWebServiceFault {
        return invoke("applyTariffToAffiliatePedestals", () -> {
            Username owner = Username.of(request.ownerUsername());
            return new ApplyTariffToAffiliatePedestalsResponse(owner.value(),
                    getBean().applyTariffToAffiliatePedestals(caller(), owner,
                            TariffId.of(request.tariffId()), request.startDate()));
        });
    }

    @PostMapping("/setCampsiteDefaultTariff")
    public SetCampsiteDefaultTariffResponse setCampsiteDefaultTariff(
            @RequestBody SetCampsiteDefaultTariffRequest request) throws TariffWebServiceFault {
        return invoke("setCampsiteDefaultTariff", () -> {
            GroupId groupId = GroupId.of(request.groupId());
            return new SetCampsiteDefaultTariffResponse(groupId.value(), getBean().setCampsiteDefaultTariff(
                    caller(), groupId, TariffId.ofNullable(request.tariffId())));
        });
    }

    @PostMapping("/findSocketTariff")
    public FindSocketTariffResponse findSocketTariff(@RequestBody FindSocketTariffRequest request)
            throws TariffWebServiceFault {
        return invoke("findSocketTariff", () -> new FindSocketTariffResponse(getBean().findSocketTariff(
                caller(), SocketId.of(request.socketId()), request.at())));
    }

    @PostMapping("/simulatePrice")
    public SimulatePriceResponse simulatePrice(@RequestBody SimulatePriceRequest request)
            throws TariffWebServiceFault {
        return invoke("simulatePrice", () -> new SimulatePriceResponse(getBean().simulatePrice(
                caller(), SocketId.of(request.socketId()),
                TariffId.ofNullable(request.tariffId()), request.plugIn(), request.plugOut(),
                request.kwh() != null ? Kwh.of(request.kwh()) : null, request.nights())));
    }

    @Override
    protected TariffWebServiceFault createFault(String message, WsErrorCode errorCode) {
        return new TariffWebServiceFault(message, errorCode);
    }
}
