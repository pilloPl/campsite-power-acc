package io.pillopl.group.attribute;

import io.pillopl.persistence.dao.LocalListAttributeDao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class LocalListAttributeIntegrationTest extends GroupAttributeIntegrationTestBase {

    @Autowired LocalListAttributeService localListAttributeService;
    @Autowired LocalListAttributeDao localListAttributeDao;

    @Test
    void localListIsInheritedFromTheMarket() {
        localListAttributeDao.saveAndFlush(on(hrSites, new LocalListAttribute(true, true, true)));

        assertThat(localListAttributeService.isLocalListGroup(section)).isTrue();
        assertThat(localListAttributeService.getLocalListAttribute(section).isIncludeHouseCards()).isTrue();
    }

    @Test
    void nearestLocalListAttributeDecidesEvenWhenAnAncestorAllowsIt() {
        localListAttributeDao.saveAndFlush(on(hrSites, new LocalListAttribute(true, true, true)));
        localListAttributeDao.saveAndFlush(on(campsite, new LocalListAttribute(false, false, false)));

        assertThat(localListAttributeService.isLocalListGroup(section)).isFalse();
        assertThat(localListAttributeService.getLocalListAttribute(section)).isNull();
    }

    @Test
    void noLocalListAttributeMeansNoLocalList() {
        assertThat(localListAttributeService.isLocalListGroup(section.getId())).isFalse();
        assertThat(localListAttributeService.isLocalListGroup(999_999L)).isFalse();
    }
}
