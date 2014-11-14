package io.pillopl.group.attribute;

import io.pillopl.group.GroupService;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.persistence.dao.NotificationDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Handles notifications in the group hierarchy: the recipient of the nearest group that has one for the type.
 */
@Service
public class NotificationService {

    private final GroupService groupService;

    private final NotificationDao notificationDao;

    public NotificationService(GroupService groupService, NotificationDao notificationDao) {
        this.groupService = groupService;
        this.notificationDao = notificationDao;
    }

    /**
     * @return the lowest notification of the given type on the parent line of the group, or null
     */
    @Transactional(readOnly = true)
    public Notification fetchNotification(Long groupId, NotificationType notificationType) {
        List<Long> parentGroupLineIds = groupService.fetchParentLineIds(groupId, true);
        if (parentGroupLineIds.isEmpty()) {
            return null;
        }
        List<Notification> notifications = notificationDao.findByGroupIds(parentGroupLineIds);
        return GroupAttributeHelper.findNearest(parentGroupLineIds, notifications,
                notification -> notification.getNotificationType() == notificationType);
    }

    /**
     * Recipient of the service mails of the given type for a pedestal: the nearest notification above its group.
     *
     * @return the notification, or null when the pedestal hangs in no group or no group above has one
     */
    @Transactional(readOnly = true)
    public Notification fetchNotification(Pedestal pedestal, NotificationType notificationType) {
        if (pedestal == null || pedestal.getGroup() == null) {
            return null;
        }
        return fetchNotification(pedestal.getGroup().getId(), notificationType);
    }

    @Transactional
    public Notification persist(Notification notification) {
        return notificationDao.saveAndFlush(notification);
    }

    @Transactional
    public void deleteById(Long notificationId) {
        notificationDao.deleteById(notificationId);
    }
}
