package io.pillopl.infrastructure;

import io.pillopl.group.Group;
import io.pillopl.group.GroupServiceHelper;
import io.pillopl.group.GroupType;
import io.pillopl.persistence.dao.CommissioningDao;
import io.pillopl.persistence.dao.UserinfoDao;
import io.pillopl.user.Userinfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Open commissionings: created by staff, visible to the users managing the affiliate owner, found by the installer
 * through the token.
 */
@Service
@Transactional
public class CommissioningService {

    private static final Logger LOG = LoggerFactory.getLogger(CommissioningService.class);

    private final CommissioningDao commissioningDao;

    private final UserinfoDao userinfoDao;

    private final LocationService locationService;

    public CommissioningService(CommissioningDao commissioningDao, UserinfoDao userinfoDao,
                                LocationService locationService) {
        this.commissioningDao = commissioningDao;
        this.userinfoDao = userinfoDao;
        this.locationService = locationService;
    }

    public Commissioning create(Commissioning commissioning) {
        commissioning.setCreateDate(LocalDateTime.now());
        commissioning.setToken(UUID.randomUUID().toString());
        commissioning.getLocation().setDeleted(false);
        locationService.save(commissioning.getLocation());
        return commissioningDao.save(commissioning);
    }

    /**
     * Open commissionings whose affiliate owner hangs in the STAFF subtree of the user (in-memory walk).
     */
    @Transactional(readOnly = true)
    public List<Commissioning> findAll(String username) {
        List<Commissioning> commissionings = new ArrayList<>();

        Userinfo userinfo = userinfoDao.findByUsername(username);
        if (userinfo == null) {
            return commissionings;
        }

        List<Commissioning> allOpen = commissioningDao.findAllOpen(); // only the open ones, should not be many

        // check access
        if (!allOpen.isEmpty()) {
            Group staffGroup = userinfo.getGroup(GroupType.STAFF);
            List<Group> manageableGroups = GroupServiceHelper.extractAllManageableGroupsForGroup(staffGroup);
            for (Commissioning commissioning : allOpen) {
                if (manageableGroups.contains(commissioning.getAffiliateOwner().getGroup())) {
                    commissionings.add(commissioning);
                }
            }
        }
        return commissionings;
    }

    /**
     * @return the open commissioning, or null when the installer or the token is unknown or the installer has no
     * access to the affiliate owner
     */
    @Transactional(readOnly = true)
    public Commissioning findByToken(String installerUsername, String token) {
        Userinfo installer = userinfoDao.findByUsername(installerUsername);
        Commissioning commissioning = commissioningDao.findOpenByToken(token);
        if (installer == null || commissioning == null) {
            return null;
        }

        // check access
        if (checkInstallerRights(installer, commissioning.getAffiliateOwner())) {
            return commissioning;
        } else {
            LOG.info("No access to commissioning with token: " + token);
            return null;
        }
    }

    /**
     * The installer may commission for owners whose own group lies in the installer's STAFF subtree.
     */
    @Transactional(readOnly = true)
    public boolean checkInstallerRights(Userinfo installer, Userinfo affiliateOwner) {
        List<Group> manageableGroups = GroupServiceHelper.extractAllManageableGroupsForGroup(
                installer.getGroup(GroupType.STAFF));
        return manageableGroups.contains(affiliateOwner.getGroup());
    }

    public Commissioning finish(Commissioning commissioning) {
        commissioning.finish(LocalDateTime.now());
        return commissioningDao.save(commissioning);
    }
}
