package io.pillopl.webservice.console.card;

import io.pillopl.api.CardApi;
import io.pillopl.value.CardId;
import io.pillopl.value.CardRfid;
import io.pillopl.value.PedestalCode;
import io.pillopl.value.SocketLinkId;
import io.pillopl.value.Username;
import io.pillopl.webservice.console.common.AbstractWebService;
import io.pillopl.webservice.console.common.WebServicePaths;
import io.pillopl.webservice.console.common.WsErrorCode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web service of the cards tab: card lookup, issue at the reception, block, validity, check at a socket, and the
 * local card lists of the pedestals.
 */
@RestController
@RequestMapping(WebServicePaths.CARD)
public class CardWebService extends AbstractWebService<CardApi, CardWebServiceFault> {

    public CardWebService(CardApi cardApi) {
        super(cardApi);
    }

    @PostMapping("/findCards")
    public FindCardsResponse findCards(@RequestBody FindCardsRequest request) throws CardWebServiceFault {
        return invoke("findCards", () -> new FindCardsResponse(getBean().findCards(
                Username.of(request.authUsername()), request.rfidOrNumber())));
    }

    @PostMapping("/issueCard")
    public IssueCardResponse issueCard(@RequestBody IssueCardRequest request) throws CardWebServiceFault {
        return invoke("issueCard", () -> new IssueCardResponse(getBean().issueCard(
                Username.of(request.authUsername()), request.card())));
    }

    @PostMapping("/changeCardStatus")
    public ChangeCardStatusResponse changeCardStatus(@RequestBody ChangeCardStatusRequest request)
            throws CardWebServiceFault {
        return invoke("changeCardStatus", () -> new ChangeCardStatusResponse(getBean().changeCardStatus(
                Username.of(request.authUsername()), CardId.of(request.cardId()), request.activation())));
    }

    @PostMapping("/changeCardValidity")
    public ChangeCardValidityResponse changeCardValidity(@RequestBody ChangeCardValidityRequest request)
            throws CardWebServiceFault {
        return invoke("changeCardValidity", () -> new ChangeCardValidityResponse(getBean().changeCardValidity(
                Username.of(request.authUsername()), CardId.of(request.cardId()), request.validFrom(),
                request.validTo())));
    }

    @PostMapping("/checkCardAtSocket")
    public CheckCardAtSocketResponse checkCardAtSocket(@RequestBody CheckCardAtSocketRequest request)
            throws CardWebServiceFault {
        return invoke("checkCardAtSocket", () -> new CheckCardAtSocketResponse(getBean().checkCardAtSocket(
                Username.of(request.authUsername()), CardRfid.of(request.cardRfid()),
                SocketLinkId.of(request.pedestalCode(), request.socketNo()))));
    }

    @PostMapping("/getLocalCardList")
    public GetLocalCardListResponse getLocalCardList(@RequestBody GetLocalCardListRequest request)
            throws CardWebServiceFault {
        return invoke("getLocalCardList", () -> new GetLocalCardListResponse(getBean().getLocalCardList(
                Username.of(request.authUsername()), PedestalCode.of(request.pedestalCode()))));
    }

    @PostMapping("/sendLocalCardList")
    public SendLocalCardListResponse sendLocalCardList(@RequestBody SendLocalCardListRequest request)
            throws CardWebServiceFault {
        return invoke("sendLocalCardList", () -> {
            PedestalCode code = PedestalCode.of(request.pedestalCode());
            boolean sent = getBean().sendLocalCardList(Username.of(request.authUsername()), code);
            return new SendLocalCardListResponse(code.value(), sent);
        });
    }

    @Override
    protected CardWebServiceFault createFault(String message, WsErrorCode errorCode) {
        return new CardWebServiceFault(message, errorCode);
    }
}
