package io.pillopl.webservice.console.guest;

import io.pillopl.api.GuestApi;
import io.pillopl.value.CardId;
import io.pillopl.value.GroupId;
import io.pillopl.value.PitchNo;
import io.pillopl.value.Username;
import io.pillopl.webservice.console.common.AbstractWebService;
import io.pillopl.webservice.console.common.WebServicePaths;
import io.pillopl.webservice.console.common.WsErrorCode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web service of the reception's stays: check-in and check-out.
 */
@RestController
@RequestMapping(WebServicePaths.GUEST)
public class GuestWebService extends AbstractWebService<GuestApi, GuestWebServiceFault> {

    public GuestWebService(GuestApi guestApi) {
        super(guestApi);
    }

    @PostMapping("/checkIn")
    public CheckInResponse checkIn(@RequestBody CheckInRequest request) throws GuestWebServiceFault {
        return invoke("checkIn", () -> new CheckInResponse(getBean().checkIn(Username.of(request.authUsername()),
                request.guestId(), request.campsiteGroupId() != null ? GroupId.of(request.campsiteGroupId()) : null,
                PitchNo.ofNullable(request.pitchNo()), request.arrival(), request.departure(),
                request.cardId() != null ? CardId.of(request.cardId()) : null, request.unitId(),
                request.billingAccountNo())));
    }

    @PostMapping("/checkOut")
    public CheckOutResponse checkOut(@RequestBody CheckOutRequest request) throws GuestWebServiceFault {
        return invoke("checkOut", () -> new CheckOutResponse(getBean().checkOut(Username.of(request.authUsername()),
                request.stayId())));
    }

    @Override
    protected GuestWebServiceFault createFault(String message, WsErrorCode errorCode) {
        return new GuestWebServiceFault(message, errorCode);
    }
}
