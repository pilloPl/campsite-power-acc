package io.pillopl.group.attribute;

import io.pillopl.persistence.dao.NotificationDao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationIntegrationTest extends GroupAttributeIntegrationTestBase {

    @Autowired NotificationService notificationService;
    @Autowired NotificationDao notificationDao;

    @Test
    void nearestNotificationOfTheRequestedTypeIsTheRecipient() {
        notificationService.persist(on(hrSites, new Notification("Service HR", "service@hr.example",
                NotificationType.PEDESTAL_STATUS)));
        notificationService.persist(on(campsite, new Notification("Bay reception", "reception@bay.example",
                NotificationType.FIRMWARE_STATUS)));

        assertThat(notificationService.fetchNotification(section.getId(), NotificationType.PEDESTAL_STATUS).getEmail())
                .isEqualTo("service@hr.example");
        assertThat(notificationService.fetchNotification(section.getId(), NotificationType.FIRMWARE_STATUS).getEmail())
                .isEqualTo("reception@bay.example");
        assertThat(notificationService.fetchNotification(section.getId(), NotificationType.DIAGNOSTICS_STATUS))
                .isNull();
    }

    @Test
    void oneNotificationPerGroupAndType() {
        notificationDao.saveAndFlush(on(campsite, new Notification("A", "a@bay.example", NotificationType.PEDESTAL_STATUS)));

        assertThatThrownBy(() -> notificationDao.saveAndFlush(
                on(campsite, new Notification("B", "b@bay.example", NotificationType.PEDESTAL_STATUS))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletedNotificationNoLongerReceivesMails() {
        Notification notification = notificationService.persist(on(campsite,
                new Notification("Bay reception", "reception@bay.example", NotificationType.PEDESTAL_STATUS)));

        notificationService.deleteById(notification.getId());

        assertThat(notificationService.fetchNotification(section.getId(), NotificationType.PEDESTAL_STATUS)).isNull();
    }
}
