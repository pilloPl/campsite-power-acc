package io.pillopl.webservice.console.guest;

import io.pillopl.api.GuestApi;
import io.pillopl.dto.guest.GuestDTO;
import io.pillopl.value.CardId;
import io.pillopl.value.GroupId;
import io.pillopl.value.PitchNo;
import io.pillopl.webservice.console.common.AbstractWebService;
import io.pillopl.webservice.console.common.WebServicePaths;
import io.pillopl.webservice.console.common.WsErrorCode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web service of the guests tab: guests, their page, stays and seasonal contracts; check-in, check-out, new
 * departure, new guests and units, cancelled orders.
 */
@RestController
@RequestMapping(WebServicePaths.GUEST)
public class GuestWebService extends AbstractWebService<GuestApi, GuestWebServiceFault> {

    public GuestWebService(GuestApi guestApi) {
        super(guestApi);
    }

    @PostMapping("/checkIn")
    public CheckInResponse checkIn(@RequestBody CheckInRequest request) throws GuestWebServiceFault {
        return invoke("checkIn", () -> new CheckInResponse(getBean().checkIn(caller(),
                request.guestId(), request.campsiteGroupId() != null ? GroupId.of(request.campsiteGroupId()) : null,
                PitchNo.ofNullable(request.pitchNo()), request.arrival(), request.departure(),
                request.cardId() != null ? CardId.of(request.cardId()) : null, request.unitId(),
                request.billingAccountNo())));
    }

    @PostMapping("/checkOut")
    public CheckOutResponse checkOut(@RequestBody CheckOutRequest request) throws GuestWebServiceFault {
        return invoke("checkOut", () -> new CheckOutResponse(getBean().checkOut(caller(),
                request.stayId())));
    }

    @PostMapping("/findGuests")
    public FindGuestsResponse findGuests(@RequestBody FindGuestsRequest request) throws GuestWebServiceFault {
        return invoke("findGuests", () -> new FindGuestsResponse(getBean().findGuests(caller(), request.search())));
    }

    @PostMapping("/getGuest")
    public GetGuestResponse getGuest(@RequestBody GetGuestRequest request) throws GuestWebServiceFault {
        return invoke("getGuest", () -> new GetGuestResponse(getBean().getGuest(caller(), request.guestId())));
    }

    @PostMapping("/findStays")
    public FindStaysResponse findStays(@RequestBody FindStaysRequest request) throws GuestWebServiceFault {
        return invoke("findStays", () -> new FindStaysResponse(getBean().findStays(caller(),
                request.campsiteGroupId() != null ? GroupId.of(request.campsiteGroupId()) : null, request.state(),
                request.from(), request.to())));
    }

    @PostMapping("/findSeasonalContracts")
    public FindSeasonalContractsResponse findSeasonalContracts(@RequestBody FindSeasonalContractsRequest request)
            throws GuestWebServiceFault {
        return invoke("findSeasonalContracts", () -> new FindSeasonalContractsResponse(
                getBean().findSeasonalContracts(caller(), request.status())));
    }

    @PostMapping("/createGuest")
    public CreateGuestResponse createGuest(@RequestBody CreateGuestRequest request) throws GuestWebServiceFault {
        return invoke("createGuest", () -> new CreateGuestResponse(getBean().createGuest(caller(),
                request.issuerGroupId() != null ? GroupId.of(request.issuerGroupId()) : null,
                new GuestDTO(null, request.issuerGroupId(), null, request.guestType(), request.name(),
                        request.firstName(), null, request.email(), request.phone(), request.city(),
                        request.countryIso(), null, null, request.carPlate(), request.trailerPlate(),
                        request.paymentMethod(), null))));
    }

    @PostMapping("/addUnit")
    public AddUnitResponse addUnit(@RequestBody AddUnitRequest request) throws GuestWebServiceFault {
        return invoke("addUnit", () -> new AddUnitResponse(getBean().addUnit(caller(), request.guestId(),
                request.unitType(), request.plate(), request.lengthM())));
    }

    @PostMapping("/changeDeparture")
    public ChangeDepartureResponse changeDeparture(@RequestBody ChangeDepartureRequest request)
            throws GuestWebServiceFault {
        return invoke("changeDeparture", () -> new ChangeDepartureResponse(getBean().changeDeparture(caller(),
                request.stayId(), request.departure())));
    }

    @PostMapping("/cancelOrder")
    public CancelOrderResponse cancelOrder(@RequestBody CancelOrderRequest request) throws GuestWebServiceFault {
        return invoke("cancelOrder", () -> new CancelOrderResponse(getBean().cancelOrder(caller(), request.orderId(),
                request.reason())));
    }

    @Override
    protected GuestWebServiceFault createFault(String message, WsErrorCode errorCode) {
        return new GuestWebServiceFault(message, errorCode);
    }
}
