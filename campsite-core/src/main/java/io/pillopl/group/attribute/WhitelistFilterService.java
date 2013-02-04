package io.pillopl.group.attribute;

import io.pillopl.group.GroupService;
import io.pillopl.persistence.dao.WhitelistFilterDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Whether the cards of an ISSUER group take part in the card exchange with issuer partners. Per direction the
 * nearest filter covering that direction decides; no filter means synchronisable.
 */
@Service
public class WhitelistFilterService {

    private final GroupService groupService;

    private final WhitelistFilterDao whitelistFilterDao;

    public WhitelistFilterService(GroupService groupService, WhitelistFilterDao whitelistFilterDao) {
        this.groupService = groupService;
        this.whitelistFilterDao = whitelistFilterDao;
    }

    @Transactional(readOnly = true)
    public boolean isSynchronizableForImport(Long issuerGroupIdFromCard) {
        WhitelistFilter whitelistFilter = getWhitelistFilterForImport(issuerGroupIdFromCard);
        if (whitelistFilter != null) {
            return whitelistFilter.isSynchronizable();
        }
        return true;
    }

    @Transactional(readOnly = true)
    public boolean isSynchronizableForExport(Long issuerGroupIdFromCard) {
        WhitelistFilter whitelistFilter = getWhitelistFilterForExport(issuerGroupIdFromCard);
        if (whitelistFilter != null) {
            return whitelistFilter.isSynchronizable();
        }
        return true;
    }

    @Transactional(readOnly = true)
    public WhitelistFilter getWhitelistFilterForImport(Long issuerGroupIdFromCard) {
        List<Long> parentLineIds = groupService.fetchParentLineIds(issuerGroupIdFromCard, true);
        if (parentLineIds.isEmpty()) {
            return null;
        }
        List<WhitelistFilter> whitelistFilters = whitelistFilterDao.findByGroupIds(parentLineIds);
        return GroupAttributeHelper.findNearest(parentLineIds, whitelistFilters,
                filter -> WhitelistFilterType.isImportWhitelistFilterType(filter.getWhitelistFilterType()));
    }

    @Transactional(readOnly = true)
    public WhitelistFilter getWhitelistFilterForExport(Long issuerGroupIdFromCard) {
        List<Long> parentLineIds = groupService.fetchParentLineIds(issuerGroupIdFromCard, true);
        if (parentLineIds.isEmpty()) {
            return null;
        }
        List<WhitelistFilter> whitelistFilters = whitelistFilterDao.findByGroupIds(parentLineIds);
        return GroupAttributeHelper.findNearest(parentLineIds, whitelistFilters,
                filter -> WhitelistFilterType.isExportWhitelistFilterType(filter.getWhitelistFilterType()));
    }
}
