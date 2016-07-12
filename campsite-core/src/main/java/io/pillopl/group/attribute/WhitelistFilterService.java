package io.pillopl.group.attribute;

import io.pillopl.group.GroupService;
import io.pillopl.issuer.IssuerPartner;
import io.pillopl.persistence.dao.WhitelistFilterDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Predicate;

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
        return whitelistFilter == null || whitelistFilter.isSynchronizable();
    }

    @Transactional(readOnly = true)
    public boolean isSynchronizableForExport(Long issuerGroupIdFromCard) {
        WhitelistFilter whitelistFilter = exportFilterOf(issuerGroupIdFromCard);
        return whitelistFilter == null || whitelistFilter.isSynchronizable();
    }

    /**
     * The gate of the card import from one partner: the nearest import filter that targets the partner (or every
     * partner) decides; no filter means synchronisable.
     */
    @Transactional(readOnly = true)
    public boolean isSynchronizableForImport(Long issuerGroupIdFromCard, IssuerPartner partner) {
        WhitelistFilter whitelistFilter = findNearest(issuerGroupIdFromCard,
                filter -> WhitelistFilterType.isImportFilter(filter.getWhitelistFilterType())
                        && filter.isTargeting(partner));
        return whitelistFilter == null || whitelistFilter.isSynchronizable();
    }

    /**
     * The gate of the card list export to one partner, same rule as the import.
     */
    @Transactional(readOnly = true)
    public boolean isSynchronizableForExport(Long issuerGroupIdFromCard, IssuerPartner partner) {
        WhitelistFilter whitelistFilter = findNearest(issuerGroupIdFromCard,
                filter -> WhitelistFilterType.isExportFilter(filter.getWhitelistFilterType())
                        && filter.isTargeting(partner));
        return whitelistFilter == null || whitelistFilter.isSynchronizable();
    }

    private WhitelistFilter findNearest(Long issuerGroupIdFromCard, Predicate<WhitelistFilter> applies) {
        List<Long> parentLineIds = groupService.fetchParentLineIds(issuerGroupIdFromCard, true);
        if (parentLineIds.isEmpty()) {
            return null;
        }
        return GroupAttributeHelper.findNearest(parentLineIds, whitelistFilterDao.findByGroupIds(parentLineIds),
                applies);
    }

    @Transactional(readOnly = true)
    public WhitelistFilter getWhitelistFilterForImport(Long issuerGroupIdFromCard) {
        List<Long> parentLineIds = groupService.fetchParentLineIds(issuerGroupIdFromCard, true);
        if (parentLineIds.isEmpty()) {
            return null;
        }
        List<WhitelistFilter> whitelistFilters = whitelistFilterDao.findByGroupIds(parentLineIds);
        return GroupAttributeHelper.findNearest(parentLineIds, whitelistFilters,
                filter -> WhitelistFilterType.isImportFilter(filter.getWhitelistFilterType()));
    }

    @Transactional(readOnly = true)
    public WhitelistFilter exportFilterOf(Long issuerGroupIdFromCard) {
        List<Long> parentLineIds = groupService.fetchParentLineIds(issuerGroupIdFromCard, true);
        if (parentLineIds.isEmpty()) {
            return null;
        }
        List<WhitelistFilter> whitelistFilters = whitelistFilterDao.findByGroupIds(parentLineIds);
        return GroupAttributeHelper.findNearest(parentLineIds, whitelistFilters,
                filter -> WhitelistFilterType.isExportFilter(filter.getWhitelistFilterType()));
    }
}
