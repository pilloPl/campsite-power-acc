package io.pillopl.card;

import io.pillopl.api.HouseCardApi;
import io.pillopl.api.exception.InvalidInputException;
import io.pillopl.api.exception.ObjectNotFoundException;
import io.pillopl.dto.card.HouseCardDTO;
import io.pillopl.user.PrincipalResolver;
import io.pillopl.user.Userinfo;
import io.pillopl.user.UserrightName;
import io.pillopl.util.ApiExceptionTranslator;
import io.pillopl.util.ApiValues;
import io.pillopl.value.CardRfid;
import io.pillopl.value.HouseCardId;
import io.pillopl.value.Username;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * House card calls of the console on top of {@link HouseCardService}: CARD_VIEW to list, CARD_EDIT to change, and the
 * caller must be the owner or manage the owner.
 */
@Service
@Transactional
public class HouseCardApiService implements HouseCardApi {

    private final HouseCardService houseCardService;

    private final PrincipalResolver principalResolver;

    public HouseCardApiService(HouseCardService houseCardService, PrincipalResolver principalResolver) {
        this.houseCardService = houseCardService;
        this.principalResolver = principalResolver;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HouseCardDTO> findHouseCards(Username authUsername, Username ownerUsername) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.CARD_VIEW);
            Userinfo owner = principalResolver.findUser(ownerUsername);
            return HouseCardMapper.toDTOs(houseCardService.getHouseCards(principal.getUsername(),
                    owner.getUsername()));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public HouseCardDTO saveHouseCard(Username authUsername, Username ownerUsername, HouseCardDTO houseCard) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.CARD_EDIT);
            Userinfo owner = principalResolver.findUser(ownerUsername);
            if (houseCard == null) {
                throw new InvalidInputException("House card data is missing");
            }
            HouseCardType type = ApiValues.parseEnum(HouseCardType.class, houseCard.houseCardType(),
                    "house card type");
            if (houseCard.id() != null && houseCardService.getHouseCard(houseCard.id()) == null) {
                throw ObjectNotFoundException.of("House card", houseCard.id());
            }
            HouseCard saved = houseCardService.saveHouseCard(principal.getUsername(), owner.getUsername(),
                    houseCard.id(), type, CardRfid.of(houseCard.rfid()).value(), houseCard.cardNumber(),
                    houseCard.active());
            return HouseCardMapper.toDTO(saved);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public void deleteHouseCard(Username authUsername, Username ownerUsername, HouseCardId houseCardId) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.CARD_EDIT);
            Userinfo owner = principalResolver.findUser(ownerUsername);
            if (!houseCardService.deleteHouseCard(principal.getUsername(), owner.getUsername(),
                    houseCardId.value())) {
                throw ObjectNotFoundException.of("House card", houseCardId);
            }
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public void changeHouseCardStatus(Username authUsername, Username ownerUsername, HouseCardId houseCardId,
                                      boolean active) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.CARD_EDIT);
            Userinfo owner = principalResolver.findUser(ownerUsername);
            boolean changed = active
                    ? houseCardService.activateHouseCard(principal.getUsername(), owner.getUsername(),
                    houseCardId.value())
                    : houseCardService.deactivateHouseCard(principal.getUsername(), owner.getUsername(),
                    houseCardId.value());
            if (!changed) {
                throw ObjectNotFoundException.of("House card", houseCardId);
            }
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }
}
