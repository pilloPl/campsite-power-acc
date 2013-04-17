package io.pillopl.group.attribute;

import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.persistence.dao.LocalListAttributeDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Local card list in the pedestal controller: decided by the nearest {@link LocalListAttribute} on the parent line.
 */
@Service
public class LocalListAttributeService {

    private final GroupService groupService;

    private final LocalListAttributeDao localListAttributeDao;

    public LocalListAttributeService(GroupService groupService, LocalListAttributeDao localListAttributeDao) {
        this.groupService = groupService;
        this.localListAttributeDao = localListAttributeDao;
    }

    @Transactional(readOnly = true)
    public boolean isLocalListGroup(Long groupId) {
        LocalListAttribute nearest = findNearest(groupId);
        return nearest != null && nearest.isLocalListAllowed();
    }

    @Transactional(readOnly = true)
    public boolean isLocalListGroup(Group group) {
        if (group == null || group.getId() == null) {
            throw new IllegalArgumentException("Group or Group.id can't be null");
        }
        return isLocalListGroup(group.getId());
    }

    /**
     * @return the nearest attribute when it allows a local list, otherwise null
     */
    @Transactional(readOnly = true)
    public LocalListAttribute getLocalListAttribute(Group group) {
        if (group == null || group.getId() == null) {
            throw new IllegalArgumentException("Group or Group.id can't be null");
        }
        LocalListAttribute nearest = findNearest(group.getId());
        if (nearest != null && nearest.isLocalListAllowed()) {
            return nearest;
        }
        return null;
    }

    /**
     * A pedestal gets a local card list when its controller keeps one and its group allows it.
     */
    @Transactional(readOnly = true)
    public boolean isLocalListPedestal(Pedestal pedestal) {
        if (pedestal == null || pedestal.getGroup() == null) {
            return false;
        }
        if (pedestal.getPedestalType() == null || !pedestal.getPedestalType().isLocalCardList()) {
            return false;
        }
        return isLocalListGroup(pedestal.getGroup());
    }

    private LocalListAttribute findNearest(Long groupId) {
        List<Long> parentGroupLineIds = groupService.fetchParentLineIds(groupId, true);
        if (parentGroupLineIds.isEmpty()) {
            return null;
        }
        List<LocalListAttribute> list = localListAttributeDao.findByGroupIds(parentGroupLineIds);
        return GroupAttributeHelper.findNearest(parentGroupLineIds, list);
    }
}
