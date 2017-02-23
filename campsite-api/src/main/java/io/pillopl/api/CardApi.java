package io.pillopl.api;

import io.pillopl.dto.card.CampingCardDTO;
import io.pillopl.dto.card.CardCheckDTO;
import io.pillopl.dto.card.IssueCardDTO;
import io.pillopl.dto.card.LocalCardListDTO;
import io.pillopl.value.CardId;
import io.pillopl.value.CardRfid;
import io.pillopl.value.PedestalCode;
import io.pillopl.value.SocketLinkId;
import io.pillopl.value.Username;

import java.time.LocalDate;
import java.util.List;

/**
 * Camping cards of the guests in the ISSUER scope of the calling user, and the local card lists of the pedestals in
 * its SITE scope.
 */
public interface CardApi {

    /**
     * @param rfidOrNumber the UID from the reader or the number printed on the card
     */
    List<CampingCardDTO> findCards(Username authUsername, String rfidOrNumber);

    CampingCardDTO issueCard(Username authUsername, IssueCardDTO card);

    /**
     * @param activation ACTIVATED or DEACTIVATED
     */
    CampingCardDTO changeCardStatus(Username authUsername, CardId cardId, String activation);

    CampingCardDTO changeCardValidity(Username authUsername, CardId cardId, LocalDate validFrom, LocalDate validTo);

    /**
     * Free cards of the welcome card pools the user manages (cards kept for guests who arrive without a card).
     */
    List<CampingCardDTO> findWelcomePoolCards(Username authUsername);

    /**
     * Hands a card of the welcome card pool to a guest for the validity (from today when the first day is missing).
     *
     * @throws io.pillopl.api.exception.ConflictException when the card is not a free welcome pool card
     */
    CampingCardDTO assignWelcomeCard(Username authUsername, CardId cardId, Long guestId, LocalDate validFrom,
                                     LocalDate validTo);

    /**
     * What the pedestal would answer to the card at the socket now; nothing is started.
     */
    CardCheckDTO checkCardAtSocket(Username authUsername, CardRfid cardRfid, SocketLinkId socket);

    LocalCardListDTO getLocalCardList(Username authUsername, PedestalCode pedestalCode);

    /**
     * @return true when the list was sent, false when the pedestal keeps no local list
     */
    boolean sendLocalCardList(Username authUsername, PedestalCode pedestalCode);
}
