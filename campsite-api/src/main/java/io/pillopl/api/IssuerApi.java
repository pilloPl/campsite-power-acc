package io.pillopl.api;

import io.pillopl.dto.admin.IssuerPartnerDTO;
import io.pillopl.dto.admin.IssuerSyncStatusDTO;
import io.pillopl.dto.admin.WhitelistFilterDTO;
import io.pillopl.dto.card.CampingCardDTO;
import io.pillopl.value.Username;

import java.util.List;

/**
 * The exchange of card lists and consumption records with the issuer partners, for the administration: partners
 * and the sync state matrix need REPORT_VIEW, editing partners and whitelist filters and running the exchange
 * CONFIG_EDIT (the partners are network configuration; filters of groups in the caller's ISSUER scope), sending a card again BILLING_EDIT and the card in the caller's ISSUER scope.
 */
public interface IssuerApi {

    /**
     * Runs the exchange with every active partner now (the job runs it as well).
     *
     * @return the sync status of every direction of every active partner after the run
     */
    List<IssuerSyncStatusDTO> runIssuerExchange(Username authUsername);

    List<IssuerPartnerDTO> findIssuerPartners(Username authUsername);

    /**
     * Creates (id null) or changes a partner; the rules of the table apply (unique name, an ISSUER group, a SITE group
     * for the card export and the record import, an endpoint).
     */
    IssuerPartnerDTO saveIssuerPartner(Username authUsername, IssuerPartnerDTO partner);

    /**
     * Every partner by every direction it exchanges, also directions that never ran (status NOT_RUN).
     */
    List<IssuerSyncStatusDTO> findIssuerSyncStatuses(Username authUsername);

    /**
     * The card goes to the partners again with the next card export (a change for a known card, an add for an
     * unknown one, a delete for a blocked one).
     *
     * @throws io.pillopl.api.exception.ConflictException when the card is kept out of the exchange or has nothing to
     *                                                    send
     */
    CampingCardDTO resyncCard(Username authUsername, Long cardId);

    /**
     * The whitelist filters of the ISSUER groups in the caller's ISSUER scope.
     */
    List<WhitelistFilterDTO> findWhitelistFilters(Username authUsername);

    /**
     * Sets the whitelist filter of an ISSUER group in the caller's scope (one per group: an existing one is changed);
     * the partner is optional (null = every partner).
     */
    WhitelistFilterDTO saveWhitelistFilter(Username authUsername, WhitelistFilterDTO filter);

    /**
     * Removes the filter: the group follows the nearest filter above it again (none = synchronised).
     */
    void deleteWhitelistFilter(Username authUsername, Long filterId);
}
