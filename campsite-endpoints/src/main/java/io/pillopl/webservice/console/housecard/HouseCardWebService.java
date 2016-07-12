package io.pillopl.webservice.console.housecard;

import io.pillopl.api.HouseCardApi;
import io.pillopl.value.HouseCardId;
import io.pillopl.value.Username;
import io.pillopl.webservice.console.common.AbstractWebService;
import io.pillopl.webservice.console.common.WebServicePaths;
import io.pillopl.webservice.console.common.WsErrorCode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web service of the house cards of a campsite (self-service of the affiliate owner, campsite managers).
 */
@RestController
@RequestMapping(WebServicePaths.HOUSE_CARD)
public class HouseCardWebService extends AbstractWebService<HouseCardApi, HouseCardWebServiceFault> {

    public HouseCardWebService(HouseCardApi houseCardApi) {
        super(houseCardApi);
    }

    @PostMapping("/findHouseCards")
    public FindHouseCardsResponse findHouseCards(@RequestBody FindHouseCardsRequest request)
            throws HouseCardWebServiceFault {
        return invoke("findHouseCards", () -> new FindHouseCardsResponse(getBean().findHouseCards(
                caller(), Username.of(request.ownerUsername()))));
    }

    @PostMapping("/saveHouseCard")
    public SaveHouseCardResponse saveHouseCard(@RequestBody SaveHouseCardRequest request)
            throws HouseCardWebServiceFault {
        return invoke("saveHouseCard", () -> new SaveHouseCardResponse(getBean().saveHouseCard(
                caller(), Username.of(request.ownerUsername()), request.houseCard())));
    }

    @PostMapping("/deleteHouseCard")
    public DeleteHouseCardResponse deleteHouseCard(@RequestBody DeleteHouseCardRequest request)
            throws HouseCardWebServiceFault {
        return invoke("deleteHouseCard", () -> {
            HouseCardId id = HouseCardId.of(request.houseCardId());
            getBean().deleteHouseCard(caller(), Username.of(request.ownerUsername()), id);
            return new DeleteHouseCardResponse(id.value(), true);
        });
    }

    @PostMapping("/changeHouseCardStatus")
    public ChangeHouseCardStatusResponse changeHouseCardStatus(@RequestBody ChangeHouseCardStatusRequest request)
            throws HouseCardWebServiceFault {
        return invoke("changeHouseCardStatus", () -> {
            HouseCardId id = HouseCardId.of(request.houseCardId());
            boolean active = Boolean.TRUE.equals(request.active());
            getBean().changeHouseCardStatus(caller(),
                    Username.of(request.ownerUsername()), id, active);
            return new ChangeHouseCardStatusResponse(id.value(), active);
        });
    }

    @Override
    protected HouseCardWebServiceFault createFault(String message, WsErrorCode errorCode) {
        return new HouseCardWebServiceFault(message, errorCode);
    }
}
