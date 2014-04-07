package io.pillopl.group.attribute;

import io.pillopl.group.Market;
import io.pillopl.persistence.dao.DefaultTariffDao;
import io.pillopl.pricing.PricingFixture;
import io.pillopl.pricing.Tariff;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultTariffIntegrationTest extends GroupAttributeIntegrationTestBase {

    @Autowired DefaultTariffService defaultTariffService;
    @Autowired DefaultTariffDao defaultTariffDao;
    @Autowired PricingFixture pricing;

    @Test
    void defaultTariffIsAnImplementedGroupAttribute() {
        assertThat(GroupAttributeType.DEFAULT_TARIFF.isImplemented()).isTrue();
        assertThat(GroupAttributeType.DEFAULT_TARIFF.getTableName()).isEqualTo("GRP_ATTR_DEFAULT_TARIFF");
    }

    @Test
    void defaultTariffOfTheNearestGroupIsUsed() {
        Tariff network = pricing.energyTariff("NETWORK", Market.HR, "0.45");
        Tariff bay = pricing.energyTariff("BAY", Market.HR, "0.40");
        defaultTariffService.setDefaultTariff(hrSites, network);
        defaultTariffService.setDefaultTariff(campsite, bay);

        assertThat(defaultTariffService.findDefaultTariff(section.getId())).isEqualTo(bay);
        assertThat(defaultTariffService.findDefaultTariff(hrSites.getId())).isEqualTo(network);
    }

    @Test
    void settingAgainReplacesAndNullRemovesTheGroupsTariff() {
        Tariff first = pricing.energyTariff("FIRST", Market.HR, "0.45");
        Tariff second = pricing.energyTariff("SECOND", Market.HR, "0.50");
        defaultTariffService.setDefaultTariff(campsite, first);
        defaultTariffService.setDefaultTariff(campsite, second);

        assertThat(defaultTariffDao.findByGroupId(campsite.getId()).getTariff()).isEqualTo(second);

        defaultTariffService.setDefaultTariff(campsite, null);
        assertThat(defaultTariffService.findDefaultTariff(section.getId())).isNull();
    }

    @Test
    void tariffOfAnotherMarketIsRefused() {
        Tariff italian = pricing.energyTariff("LAGO", Market.IT, "0.55");

        assertThatThrownBy(() -> defaultTariffService.setDefaultTariff(campsite, italian))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("market IT");
    }

    @Test
    void onlySiteGroupsHaveADefaultTariff() {
        Tariff tariff = pricing.energyTariff("CLUB", Market.HR, "0.45");

        assertThatThrownBy(() -> defaultTariffService.setDefaultTariff(club, tariff))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("no SITE group");
    }
}
