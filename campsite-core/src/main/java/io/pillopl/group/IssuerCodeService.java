package io.pillopl.group;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issuer code printed on the cards of the network and sent in the issuer exchange, decided by the market of the
 * card's group.
 */
@Service
public class IssuerCodeService {

    public static final String UNKNOWN_ISSUER = "UNKNOWN";

    private final GroupService groupService;

    public IssuerCodeService(GroupService groupService) {
        this.groupService = groupService;
    }

    @Transactional(readOnly = true)
    public String defineIssuer(Group issuerGroup) {
        Market market = issuerGroup != null ? groupService.determineMarketByGroupId(issuerGroup.getId()) : null;
        return defineIssuer(market);
    }

    public String defineIssuer(Market market) {
        if (market == Market.HR) {
            return "CPN-HR";
        }
        if (market == Market.IT) {
            return "CPN-IT";
        }
        if (market == Market.FR) {
            return "CPN-FR";
        }
        return UNKNOWN_ISSUER;
    }
}
