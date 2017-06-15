package io.pillopl.group;

import io.pillopl.card.CampingCard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issuer code printed on the cards of the network and sent in the issuer exchange, decided by the market of the
 * card's group.
 */
@Service
public class IssuerCodeService {

    public static final String UNKNOWN_ISSUER = "UNKNOWN";

    static final String ISSUER_CODE_PREFIX = "CPN-";

    private final GroupService groupService;

    public IssuerCodeService(GroupService groupService) {
        this.groupService = groupService;
    }

    @Transactional(readOnly = true)
    public String defineIssuer(Group issuerGroup) {
        Market market = issuerGroup != null ? groupService.determineMarketByGroupId(issuerGroup.getId()) : null;
        return defineIssuer(market);
    }

    /**
     * Issuer code of a card: decided by the ISSUER group of the card's guest.
     */
    @Transactional(readOnly = true)
    public String defineIssuer(CampingCard card) {
        Group issuerGroup = card != null && card.getGuest() != null ? card.getGuest().getGroup() : null;
        return defineIssuer(issuerGroup);
    }

    /**
     * The network prefix followed by the market, the unknown issuer for a group without a market prefix.
     */
    public String defineIssuer(Market market) {
        if (market == null || !market.isPrefixMarket()) {
            return UNKNOWN_ISSUER;
        }
        return ISSUER_CODE_PREFIX + market.name();
    }
}
