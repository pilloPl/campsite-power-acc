package io.pillopl.group.attribute;

import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.GroupType;
import io.pillopl.issuer.IssuerPartner;
import io.pillopl.persistence.dao.WhitelistFilterDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
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

    /**
     * The filters set on the groups of the ISSUER subtree, ordered by group name.
     */
    @Transactional(readOnly = true)
    public List<WhitelistFilter> findFiltersOfSubtree(Group issuerScope) {
        if (issuerScope == null) {
            return List.of();
        }
        List<WhitelistFilter> filters = new ArrayList<>(whitelistFilterDao.findByGroupIds(
                groupService.findManageable(issuerScope)));
        filters.sort(Comparator.comparing(filter -> filter.getGroup().getName()));
        return filters;
    }

    /**
     * Sets the filter of an ISSUER group (a group has at most one filter: an existing one is changed).
     */
    @Transactional
    public WhitelistFilter saveFilter(Group issuerGroup, boolean synchronizable, WhitelistFilterType type,
                                      IssuerPartner partner) {
        if (issuerGroup == null) {
            throw new IllegalArgumentException("A whitelist filter needs its ISSUER group");
        }
        if (!GroupType.ISSUER.isGroupType(issuerGroup)) {
            throw new IllegalArgumentException(String.format("A whitelist filter belongs to an ISSUER group, "
                    + "not to %s group %s", issuerGroup.getType(), issuerGroup.getName()));
        }
        WhitelistFilter filter = whitelistFilterDao.findForGroup(issuerGroup.getId());
        if (filter == null) {
            filter = new WhitelistFilter();
            filter.setGroup(issuerGroup);
        }
        filter.setSynchronizable(synchronizable);
        filter.setWhitelistFilterType(type != null ? type : WhitelistFilterType.IMPORT_EXPORT);
        filter.setIssuerPartner(partner);
        return whitelistFilterDao.saveAndFlush(filter);
    }

    @Transactional
    public void deleteFilter(WhitelistFilter filter) {
        whitelistFilterDao.delete(filter);
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
        return findNearest(issuerGroupIdFromCard,
                filter -> WhitelistFilterType.isImportFilter(filter.getWhitelistFilterType()));
    }

    @Transactional(readOnly = true)
    public WhitelistFilter exportFilterOf(Long issuerGroupIdFromCard) {
        return findNearest(issuerGroupIdFromCard,
                filter -> WhitelistFilterType.isExportFilter(filter.getWhitelistFilterType()));
    }
}
