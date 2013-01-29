package io.pillopl.group.attribute;

import io.pillopl.group.GroupService;
import io.pillopl.persistence.dao.ExportCampsiteNameDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Campsite name used in exports: the nearest {@link ExportCampsiteName} on the parent line.
 */
@Service
public class ExportCampsiteNameService {

    private final GroupService groupService;

    private final ExportCampsiteNameDao exportCampsiteNameDao;

    public ExportCampsiteNameService(GroupService groupService, ExportCampsiteNameDao exportCampsiteNameDao) {
        this.groupService = groupService;
        this.exportCampsiteNameDao = exportCampsiteNameDao;
    }

    @Transactional(readOnly = true)
    public ExportCampsiteName getExportCampsiteName(Long groupId) {
        List<Long> parentGroupLineIds = groupService.fetchParentLineIds(groupId, true);
        if (parentGroupLineIds.isEmpty()) {
            return null;
        }
        List<ExportCampsiteName> list = exportCampsiteNameDao.findByGroupIds(parentGroupLineIds);
        return GroupAttributeHelper.findNearest(parentGroupLineIds, list);
    }
}
