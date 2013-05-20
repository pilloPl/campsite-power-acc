package io.pillopl.user;

import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.GroupType;
import io.pillopl.persistence.dao.UserinfoSummaryDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Self-service user list from the materialized view USERINFO_SUMMARY, and its refresh (called by a thin job).
 */
@Service
public class UserinfoSummaryService {

    private static final Logger LOG = LoggerFactory.getLogger(UserinfoSummaryService.class);

    private final UserinfoSummaryDao userinfoSummaryDao;

    private final GroupService groupService;

    public UserinfoSummaryService(UserinfoSummaryDao userinfoSummaryDao, GroupService groupService) {
        this.userinfoSummaryDao = userinfoSummaryDao;
        this.groupService = groupService;
    }

    /**
     * Self-service users hanging in the STAFF subtree of the principal; the flags filter when not null.
     */
    @Transactional(readOnly = true)
    public Page<UserinfoSummary> findSelfServiceUsers(Userinfo principal, Boolean affiliateOwner,
                                                      Boolean seasonalResident, Pageable pageable) {
        Group staffGroup = principal.getGroup(GroupType.STAFF);
        if (staffGroup == null) {
            return Page.empty(pageable);
        }
        List<Group> groups = groupService.findManageableGroups(staffGroup);
        return userinfoSummaryDao.findByGroups(groups, affiliateOwner, seasonalResident, pageable);
    }

    @Transactional
    public void refresh() {
        long start = System.currentTimeMillis();
        userinfoSummaryDao.refresh();
        LOG.debug(String.format("USERINFO_SUMMARY refreshed in %d ms", System.currentTimeMillis() - start));
    }
}
