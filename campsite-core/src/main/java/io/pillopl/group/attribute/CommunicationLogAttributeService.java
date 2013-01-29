package io.pillopl.group.attribute;

import io.pillopl.group.GroupService;
import io.pillopl.persistence.dao.CommunicationLogAttributeDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Whether the pedestal link traffic of a SITE subtree is logged: the nearest attribute decides.
 */
@Service
public class CommunicationLogAttributeService {

    private final GroupService groupService;

    private final CommunicationLogAttributeDao communicationLogAttributeDao;

    public CommunicationLogAttributeService(GroupService groupService,
                                            CommunicationLogAttributeDao communicationLogAttributeDao) {
        this.groupService = groupService;
        this.communicationLogAttributeDao = communicationLogAttributeDao;
    }

    @Transactional(readOnly = true)
    public boolean isGroupLoggable(Long groupId) {
        List<Long> parentGroupLineIds = groupService.fetchParentLineIds(groupId, true);
        if (parentGroupLineIds.isEmpty()) {
            return false;
        }
        List<CommunicationLogAttribute> list = communicationLogAttributeDao.findByGroupIds(parentGroupLineIds);
        CommunicationLogAttribute nearest = GroupAttributeHelper.findNearest(parentGroupLineIds, list);
        return nearest != null && nearest.isActive();
    }
}
