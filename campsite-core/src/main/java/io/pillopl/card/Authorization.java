package io.pillopl.card;

import io.pillopl.card.ValidationService.ValidationResult;
import io.pillopl.event.AuthorizationEvent;
import io.pillopl.event.CheckWelcomeCardAdoptionEvent;
import io.pillopl.event.EventService;
import io.pillopl.guest.Guest;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.PedestalService;
import io.pillopl.infrastructure.Socket;
import io.pillopl.power.SectionPowerBudget;
import io.pillopl.supply.SupplySessionRegistry;
import io.pillopl.value.SocketLinkId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authorization of a card held to the reader of a pedestal: at a socket (the guest wants supply there) or at the
 * pedestal (the controller only asks whether it knows the card).
 */
@Service
public class Authorization {

    private static final Logger LOG = LoggerFactory.getLogger(Authorization.class);

    private final ValidationService validationService;

    private final EventService eventService;

    private final SectionPowerBudget sectionPowerBudget;

    private final SupplySessionRegistry supplySessionRegistry;

    private final HouseCardService houseCardService;

    private final PedestalService pedestalService;

    public Authorization(ValidationService validationService, EventService eventService,
                         SectionPowerBudget sectionPowerBudget, SupplySessionRegistry supplySessionRegistry,
                         HouseCardService houseCardService, PedestalService pedestalService) {
        this.validationService = validationService;
        this.eventService = eventService;
        this.sectionPowerBudget = sectionPowerBudget;
        this.supplySessionRegistry = supplySessionRegistry;
        this.houseCardService = houseCardService;
        this.pedestalService = pedestalService;
    }

    // CAMP-84: event to check whether the card is a welcome card the campsite may adopt as its house card
    private void createCheckWelcomeCardAdoptionEvent(String pedestalCode, String cardRfid) {
        CheckWelcomeCardAdoptionEvent event = new CheckWelcomeCardAdoptionEvent(pedestalCode, cardRfid);
        LOG.debug(String.format("Created CheckWelcomeCardAdoption event %s", event));
        eventService.send(event);
    }

    void checkForCAMP84(String cardRfid, String pedestalCode, ValidationResult validationResult) {
        // CAMP-84: only a card refused because the campsite does not accept it may be a welcome card
        if (validationResult.getStatus() == ValidationStatus.CARD_NOT_ACCEPTED_AT_SITE) {
            try {
                createCheckWelcomeCardAdoptionEvent(pedestalCode, cardRfid);
            } catch (Exception e) {
                LOG.error("Creating an event to check if the card should be adopted as house card failed.", e);
            }
        }
    }

    @Transactional
    public AuthorizationResult authenticate(final String cardRfid, final SocketLinkId socketLinkId) {
        ValidationResult validationResult = validationService.validateForNow(socketLinkId, cardRfid);

        checkForCAMP84(cardRfid, socketLinkId.getPedestalCode(), validationResult);

        AuthorizationResult result;
        if (validationResult.getStatus() != ValidationStatus.ACCEPTED) {
            result = createAuthorizationResult(validationResult);
        } else if (validationResult.getStatus() == ValidationStatus.ACCEPTED && validationResult.isHouseCardAccepted()) {
            houseCardService.recordUse(validationResult.getHouseCard());
            result = createAuthorizationResult(ValidationStatus.ACCEPTED, validationResult);
        } else if (supplySessionRegistry.hasConcurrentSession(socketLinkId, cardRfid)) {
            result = createAuthorizationResult(ValidationStatus.SOCKET_OCCUPIED, validationResult);
        } else if (!sectionPowerBudget.isWithinSectionBudget(socketLinkId)) {
            result = createAuthorizationResult(ValidationStatus.UNAVAILABLE, validationResult);
        } else {
            // the guest is authorized, the socket is ready to supply
            supplySessionRegistry.createSession(socketLinkId, cardRfid);

            result = createAuthorizationResult(ValidationStatus.ACCEPTED, validationResult);
        }

        LOG.debug(String.format("Authorization result for card %s at socket %s: %s", cardRfid, socketLinkId,
                result.getStatus()));

        createAuthorizationEvent(validationResult.getSocket(), null, cardRfid, result);
        return result;
    }

    @Transactional
    public AuthorizationResult authenticate(final String cardRfid, final String pedestalCode) {
        ValidationResult validationResult = validationService.validateForNow(pedestalCode, cardRfid);

        checkForCAMP84(cardRfid, pedestalCode, validationResult);

        if (validationResult.isHouseCardAccepted()) {
            houseCardService.recordUse(validationResult.getHouseCard());
        }
        AuthorizationResult result = createAuthorizationResult(validationResult);

        LOG.debug(String.format("Authorization result for card %s at pedestal %s: %s", cardRfid, pedestalCode,
                result.getStatus()));

        createAuthorizationEvent(null, pedestalService.findByCode(pedestalCode), cardRfid, result);

        return result;
    }

    private AuthorizationResult createAuthorizationResult(final ValidationResult validationResult) {
        return createAuthorizationResult(validationResult.getStatus(), validationResult);
    }

    private AuthorizationResult createAuthorizationResult(final ValidationStatus status,
                                                          final ValidationResult validationResult) {
        AuthorizationResult result = new AuthorizationResult();
        result.setStatus(status);
        CampingCard card = validationResult.getCard();
        if (card != null) {
            if (card.getGuest() != null) {
                Guest guest = card.getGuest();
                result.setGuestName(guest.getFullName());
            }
            if (card.getValidTo() != null) {
                result.setExpiryDate(card.getValidTo());
            }
        }
        return result;
    }

    private void createAuthorizationEvent(Socket socket, Pedestal pedestal, String cardRfid,
                                          AuthorizationResult authResult) {
        try {
            AuthorizationEvent event = new AuthorizationEvent(socket != null ? socket.getId() : null,
                    pedestal != null ? pedestal.getId() : null, cardRfid, authResult.getStatus().toString());
            LOG.debug(String.format("Created AuthorizationEvent %s", event));
            eventService.send(event);
        } catch (Exception e) {
            LOG.error("Error sending AuthorizationEvent.", e);
        }
    }
}
