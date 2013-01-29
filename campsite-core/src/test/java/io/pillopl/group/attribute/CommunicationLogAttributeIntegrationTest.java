package io.pillopl.group.attribute;

import io.pillopl.persistence.dao.CommunicationLogAttributeDao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class CommunicationLogAttributeIntegrationTest extends GroupAttributeIntegrationTestBase {

    @Autowired CommunicationLogAttributeService communicationLogAttributeService;
    @Autowired CommunicationLogAttributeDao communicationLogAttributeDao;

    @Test
    void inactiveCommunicationLogOnTheCampsiteSwitchesOffTheMarketSetting() {
        communicationLogAttributeDao.saveAndFlush(on(hrSites, new CommunicationLogAttribute("all HR", true)));
        assertThat(communicationLogAttributeService.isGroupLoggable(section.getId())).isTrue();

        communicationLogAttributeDao.saveAndFlush(on(campsite, new CommunicationLogAttribute("quiet", false)));

        assertThat(communicationLogAttributeService.isGroupLoggable(section.getId())).isFalse();
    }

    @Test
    void groupWithoutSettingOrUnknownGroupIsNotLogged() {
        assertThat(communicationLogAttributeService.isGroupLoggable(section.getId())).isFalse();
        assertThat(communicationLogAttributeService.isGroupLoggable(999_999L)).isFalse();
    }
}
