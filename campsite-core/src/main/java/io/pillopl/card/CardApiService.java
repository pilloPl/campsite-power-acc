package io.pillopl.card;

import io.pillopl.api.CardApi;
import io.pillopl.api.exception.InvalidInputException;
import io.pillopl.api.exception.ObjectNotFoundException;
import io.pillopl.dto.card.CampingCardDTO;
import io.pillopl.dto.card.CardCheckDTO;
import io.pillopl.dto.card.IssueCardDTO;
import io.pillopl.dto.card.LocalCardListDTO;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.PedestalService;
import io.pillopl.user.PrincipalResolver;
import io.pillopl.user.Userinfo;
import io.pillopl.user.UserrightName;
import io.pillopl.util.ApiExceptionTranslator;
import io.pillopl.util.ApiValues;
import io.pillopl.util.DateService;
import io.pillopl.value.CardId;
import io.pillopl.value.CardRfid;
import io.pillopl.value.PedestalCode;
import io.pillopl.value.SocketLinkId;
import io.pillopl.value.Username;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Card calls of the console on top of {@link CampingCardService} and {@link LocalCardListService}. Cards need
 * CARD_VIEW / CARD_EDIT and a guest in the ISSUER scope; local card lists need PEDESTAL_VIEW / PEDESTAL_EDIT and a
 * pedestal in the SITE scope of the calling user.
 */
@Service
@Transactional
public class CardApiService implements CardApi {

    private final CampingCardService campingCardService;

    private final LocalCardListService localCardListService;

    private final PedestalService pedestalService;

    private final PrincipalResolver principalResolver;

    private final DateService dateService;

    public CardApiService(CampingCardService campingCardService, LocalCardListService localCardListService,
                          PedestalService pedestalService, PrincipalResolver principalResolver,
                          DateService dateService) {
        this.campingCardService = campingCardService;
        this.localCardListService = localCardListService;
        this.pedestalService = pedestalService;
        this.principalResolver = principalResolver;
        this.dateService = dateService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CampingCardDTO> findCards(Username authUsername, String rfidOrNumber) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.CARD_VIEW);
            return CardMapper.toDTOs(campingCardService.findCards(principal, rfidOrNumber), dateService.getNow());
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public CampingCardDTO issueCard(Username authUsername, IssueCardDTO card) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.CARD_EDIT);
            if (card == null) {
                throw new InvalidInputException("Card data is missing");
            }
            CampingCard issued = campingCardService.issueCard(principal, card.guestId(),
                    CardRfid.of(card.cardRfid()).value(), card.cardNumber(), card.validFrom(), card.validTo(),
                    card.billingAccountNo());
            return CardMapper.toDTO(issued, dateService.getNow());
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public CampingCardDTO changeCardStatus(Username authUsername, CardId cardId, String activation) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.CARD_EDIT);
            CardActivation value = ApiValues.parseEnum(CardActivation.class, activation, "card status");
            return CardMapper.toDTO(campingCardService.changeActivation(principal, cardId.value(), value),
                    dateService.getNow());
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public CampingCardDTO changeCardValidity(Username authUsername, CardId cardId, LocalDate validFrom,
                                             LocalDate validTo) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.CARD_EDIT);
            return CardMapper.toDTO(campingCardService.changeValidity(principal, cardId.value(), validFrom, validTo),
                    dateService.getNow());
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CardCheckDTO checkCardAtSocket(Username authUsername, CardRfid cardRfid, SocketLinkId socket) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.CARD_VIEW);
            findPedestalInScope(principal, socket.pedestalCode());
            ValidationService.ValidationResult result = campingCardService.checkCardAtSocket(principal,
                    cardRfid.value(), socket);
            return CardMapper.toCheckDTO(socket.toString(), cardRfid.value(), result);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public LocalCardListDTO getLocalCardList(Username authUsername, PedestalCode pedestalCode) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.PEDESTAL_VIEW);
            findPedestalInScope(principal, pedestalCode);
            return CardMapper.toDTO(localCardListService.buildLocalCardList(pedestalCode.value()));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public boolean sendLocalCardList(Username authUsername, PedestalCode pedestalCode) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.PEDESTAL_EDIT);
            findPedestalInScope(principal, pedestalCode);
            return localCardListService.sendFull(pedestalCode.value(), false);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    private Pedestal findPedestalInScope(Userinfo principal, PedestalCode pedestalCode) {
        Pedestal pedestal = pedestalService.findByCodeForUser(principal, pedestalCode.value());
        if (pedestal == null) {
            // a pedestal outside the scope is reported as unknown
            throw ObjectNotFoundException.of("Pedestal", pedestalCode);
        }
        return pedestal;
    }
}
