package io.pillopl.group.attribute;

import io.pillopl.group.GroupService;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.persistence.dao.CommunicationLogAttributeDao;
import io.pillopl.persistence.dao.PedestalDao;
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

    private final PedestalDao pedestalDao;

    public CommunicationLogAttributeService(GroupService groupService,
                                            CommunicationLogAttributeDao communicationLogAttributeDao,
                                            PedestalDao pedestalDao) {
        this.groupService = groupService;
        this.communicationLogAttributeDao = communicationLogAttributeDao;
        this.pedestalDao = pedestalDao;
    }

    @Transactional(readOnly = true)
    public boolean isPedestalLoggable(Long pedestalId) {
        Pedestal pedestal = pedestalId != null ? pedestalDao.findById(pedestalId).orElse(null) : null;
        return isPedestalLoggable(pedestal);
    }

    @Transactional(readOnly = true)
    public boolean isPedestalLoggable(String pedestalCode) {
        Pedestal pedestal = pedestalCode != null ? pedestalDao.findByCode(pedestalCode) : null;
        return isPedestalLoggable(pedestal);
    }

    private boolean isPedestalLoggable(Pedestal pedestal) {
        if (pedestal == null || pedestal.getGroup() == null) {
            return false;
        }
        return isGroupLoggable(pedestal.getGroup().getId());
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
