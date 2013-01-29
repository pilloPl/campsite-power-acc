package io.pillopl.group.attribute;

import io.pillopl.persistence.dao.WelcomeCardAdoptionDao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class WelcomeCardAdoptionIntegrationTest extends GroupAttributeIntegrationTestBase {

    @Autowired WelcomeCardAdoptionService welcomeCardAdoptionService;
    @Autowired WelcomeCardAdoptionDao welcomeCardAdoptionDao;

    @Test
    void welcomeCardAdoptionBubblesUpToTheNearestSetting() {
        assertThat(welcomeCardAdoptionService.adoptionIsAllowedFor(section)).isFalse();

        welcomeCardAdoptionDao.saveAndFlush(on(campsite, new WelcomeCardAdoption(true)));

        assertThat(welcomeCardAdoptionService.adoptionIsAllowedFor(section)).isTrue();
        assertThat(welcomeCardAdoptionService.adoptionIsAllowedFor(hrSites)).isFalse();
    }
}
