package io.pillopl.group;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MarketExtensionTest {

    private final GroupService groupService = mock(GroupService.class);

    @Test
    void groupNamesAreComposedFromTheMarket() {
        MarketExtension<Market> it = new MarketExtension<>(Market.IT, groupService);

        assertThat(it.getIssuerRootName()).isEqualTo("IT ISSUERS");
        assertThat(it.getSiteRootName()).isEqualTo("IT SITES");
        assertThat(it.getStaffRootName()).isEqualTo("IT STAFF");
        assertThat(it.getNetworkAccessGroupName()).isEqualTo("NETWORK ACCESS IT");
        assertThat(it.getHouseCardsOnlyGroupName()).isEqualTo("HOUSE CARDS ONLY IT");
        assertThat(it.getWelcomeCardPoolGroupName()).isEqualTo("IT WELCOME_CARD_POOL");
        assertThat(it.getReceptionGroupName()).isEqualTo("IT RECEPTION");
        assertThat(it.getRootName(GroupType.SITE)).isEqualTo("IT SITES");
    }
}
