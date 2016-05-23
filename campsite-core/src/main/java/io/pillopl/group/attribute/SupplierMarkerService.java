package io.pillopl.group.attribute;

import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.group.GroupServiceHelper;
import io.pillopl.group.GroupType;
import io.pillopl.group.Market;
import io.pillopl.persistence.dao.SupplierMarkerDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Pedestal stock of a manufacturer ("SUPPLIER &lt;manufacturer&gt;" SITE groups marked with a {@link SupplierMarker}).
 */
@Service
public class SupplierMarkerService {

    private final GroupService groupService;

    private final GroupManager groupManager;

    private final SupplierMarkerDao supplierMarkerDao;

    public SupplierMarkerService(GroupService groupService, GroupManager groupManager,
                                 SupplierMarkerDao supplierMarkerDao) {
        this.groupService = groupService;
        this.groupManager = groupManager;
        this.supplierMarkerDao = supplierMarkerDao;
    }

    /**
     * Parent line, then the markers of the line, then the nearest marker decides.
     */
    @Transactional(readOnly = true)
    public boolean isInSupplierStock(Long groupId) {
        List<Long> parentGroupLineIds = groupService.fetchParentLineIds(groupId, true);
        if (parentGroupLineIds.isEmpty()) {
            return false;
        }
        List<SupplierMarker> list = supplierMarkerDao.findByGroupIds(parentGroupLineIds);
        SupplierMarker nearest = GroupAttributeHelper.findNearest(parentGroupLineIds, list);
        return nearest != null && nearest.isActive();
    }

    @Transactional(readOnly = true)
    public boolean isInSupplierStock(Group group) {
        if (group == null || group.getId() == null) {
            throw new IllegalArgumentException("A stored group is required");
        }
        return isInSupplierStock(group.getId());
    }

    /**
     * Creates "SUPPLIER &lt;manufacturer&gt;" under the market's SITE root, marked as active supplier stock.
     */
    @Transactional
    public Group createSupplierStock(String manufacturer, Market market) {
        Group siteRoot = groupService.getExtensionByMarket(market).findSiteRoot();
        if (siteRoot == null) {
            throw new IllegalStateException("No SITE root for market " + market);
        }
        String name = GroupServiceHelper.getSupplierGroupName(manufacturer);
        Group supplierGroup = groupManager.createChildGroup(siteRoot, name, name, GroupType.SITE);
        SupplierMarker marker = new SupplierMarker(manufacturer, true);
        marker.setGroup(supplierGroup);
        supplierMarkerDao.saveAndFlush(marker);
        return supplierGroup;
    }
}
