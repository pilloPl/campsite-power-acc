package io.pillopl.user;

import io.pillopl.group.AccessDeniedException;
import io.pillopl.group.AccessMode;
import io.pillopl.group.AffiliateGroupService;
import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.GroupType;
import io.pillopl.group.IsLinkedToGroup;
import io.pillopl.group.Market;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.persistence.dao.UserinfoDao;
import io.pillopl.persistence.dao.UserroleDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Console users: lookups, administrative scope over other users and guests, user set-up.
 */
@Service
@Transactional
public class UserinfoService {

    private static final Logger LOG = LoggerFactory.getLogger(UserinfoService.class);

    static final String USERNAME_SUFFIX_SEPARATOR = "-";

    private static final int USERNAME_SUFFIX_LIMIT = 999;

    private static final String DEFAULT_NAME = "tbd";

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserinfoDao userinfoDao;

    private final UserroleDao userroleDao;

    private final GroupService groupService;

    private final AffiliateGroupService affiliateGroupService;

    public UserinfoService(UserinfoDao userinfoDao, UserroleDao userroleDao, GroupService groupService,
                           AffiliateGroupService affiliateGroupService) {
        this.userinfoDao = userinfoDao;
        this.userroleDao = userroleDao;
        this.groupService = groupService;
        this.affiliateGroupService = affiliateGroupService;
    }

    public Userinfo find(Long id) {
        return userinfoDao.findById(id).orElse(null);
    }

    public Userinfo findByUsername(String username) {
        return userinfoDao.findByUsername(username);
    }

    /**
     * The market of a user is the market of its own STAFF group.
     *
     * @return the market or null (logged) when the user does not exist or has no group
     */
    public Market determineMarket(String username) {
        Userinfo userinfo = userinfoDao.findByUsername(username);
        if (userinfo == null) {
            LOG.error(String.format("User with username '%s' not exists", username));
            return null;
        }
        if (userinfo.getGroup() == null) {
            LOG.error(String.format("For user with username '%s' no group is set", username));
            return null;
        }

        return groupService.determineMarketByGroupId(userinfo.getGroup().getId());
    }

    /**
     * A guest (or anything hanging in the ISSUER tree) is manageable when its group lies in the user's ISSUER scope.
     */
    public boolean isManageable(Userinfo user, IsLinkedToGroup issuerObject) {
        return groupService.isManageable(user.getGroup(GroupType.ISSUER), issuerObject.getGroup());
    }

    /**
     * Another user is manageable when its own group lies in the user's STAFF scope.
     */
    public boolean isManageable(Userinfo user, Userinfo userIsManageable) {
        return groupService.isManageable(user.getGroup(GroupType.STAFF), userIsManageable.getGroup());
    }

    /**
     * Like {@link #isManageable(Userinfo, Userinfo)}; with introspection a user may always manage itself.
     */
    public boolean isManageable(Userinfo user, Userinfo userIsManageable, boolean introspection) {
        return introspection && user.getUsername().equals(userIsManageable.getUsername())
                || isManageable(user, userIsManageable);
    }

    public boolean checkIsManageable(Userinfo user, Userinfo userIsManageable, boolean introspection) {
        if (isManageable(user, userIsManageable, introspection)) {
            return true;
        }
        throw new AccessDeniedException("No access to user");
    }

    /**
     * @return the user to manage, or null when it does not exist
     * @throws AccessDeniedException when the user exists but is outside the scope
     */
    public Userinfo checkIsManageable(Userinfo userinfo, Long userId) {
        Objects.requireNonNull(userinfo);
        Objects.requireNonNull(userId);

        Userinfo userToManage = find(userId);
        if (userToManage == null) return null;

        checkIsManageable(userinfo, userToManage, true);
        return userToManage;
    }

    /**
     * Mail locale of new users of a market.
     */
    public String getDefaultLocale(Market market) {
        switch (market) {
            case HR:
                return "hr";
            case IT:
                return "it";
            case FR:
                return "fr";
            default:
        }
        return null;
    }

    public boolean emailExists(String email) {
        return !userinfoDao.findByEmail(email).isEmpty();
    }

    /**
     * New, not yet persisted user in status INVITED with a random password, no groups and no roles.
     */
    public Userinfo createUserinfo(String email, String name) {
        Userinfo userinfoToCreate = new Userinfo();
        userinfoToCreate.setStatus(UserinfoStatus.INVITED);
        userinfoToCreate.setEmail(email);
        userinfoToCreate.setName(name != null && !name.isBlank() ? name : DEFAULT_NAME);
        userinfoToCreate.setPasswordHash(UUID.randomUUID().toString());
        userinfoToCreate.setCreateDate(LocalDateTime.now());
        userinfoToCreate.setDeleted(false);
        userinfoToCreate.setGroups(new ArrayList<>());
        userinfoToCreate.setRoles(new ArrayList<>());
        return userinfoToCreate;
    }

    /**
     * Creates a self-service user in the "&lt;MKT&gt; SELF SERVICE" group of the market (the principal's market when
     * none is given). The principal must manage that group.
     */
    public Userinfo createUser(Userinfo principal, String email, String name, Market market) {
        Userinfo userinfoToCreate = createUserinfo(email, name);

        LOG.info("Check if user does not exists");
        if (emailExists(email)) {
            throw new IllegalStateException(String.format("User with email '%s' already exists", email));
        }

        LOG.info("Get group user should belong to from principal if not specified");
        if (market == null || Market.UNKNOWN == market) {
            market = determineMarket(principal.getUsername());
        }
        if (market == null) {
            throw new IllegalStateException("Cannot determine the market of " + principal);
        }

        Group selfServiceGroup = groupService.getExtensionByMarket(market).findSelfServiceStaffGroup();
        if (null == principal.getGroup(GroupType.STAFF)) {
            LOG.error("The principal user must have a Group of Type STAFF assigned! " + principal.getUsername());
        }
        groupService.checkIsManageable(principal.getGroup(GroupType.STAFF), selfServiceGroup);

        userinfoToCreate.setGroup(selfServiceGroup);
        userinfoToCreate.setEmailLocale(getDefaultLocale(market));
        Userrole selfServiceUser = userroleDao.findByName(UserroleName.SELF_SERVICE_USER.name());
        userinfoToCreate.getRoles().add(selfServiceUser);

        // At last we generate the username
        return saveWithGeneratedUsername(userinfoToCreate);
    }

    /**
     * Saves a new user with the local part of its email as username. When taken, "-1" ... "-998" is appended; when
     * all are taken, the database id becomes the suffix.
     */
    public Userinfo saveWithGeneratedUsername(Userinfo userinfo) {
        Objects.requireNonNull(userinfo);
        String email = userinfo.getEmail();
        if (userinfo.getUsername() != null) {
            throw new IllegalArgumentException("Because the username will be generated, we explicitly do not want one set");
        }
        if (!isEmail(email)) {
            throw new IllegalArgumentException(String.format("Not an email address: %s", email));
        }
        if (userinfo.getId() != null) {
            throw new IllegalArgumentException(String.format("Userinfo already has an id, (email: %s)", email));
        }

        String localPart = getLocalPartOfEmail(email);
        String username = localPart;
        for (int i = 1; i < USERNAME_SUFFIX_LIMIT; i++) {
            if (userinfoDao.findAllByUsernameIgnoreCase(username).isEmpty()) {
                userinfo.setUsername(username);
                userinfo = userinfoDao.save(userinfo);
                break;
            }
            username = localPart + USERNAME_SUFFIX_SEPARATOR + i;
        }

        // If the limit is exceeded we take the db id as suffix.
        if (null == userinfo.getId()) {
            LOG.warn(String.format("Usernames exceeding limit of %s, taking the db id as suffix.", USERNAME_SUFFIX_LIMIT));
            userinfo.setUsername(UUID.randomUUID().toString());
            userinfo = userinfoDao.save(userinfo);
            userinfo.setUsername(localPart + USERNAME_SUFFIX_SEPARATOR + userinfo.getId());
            userinfo = userinfoDao.save(userinfo);
        }
        return userinfo;
    }

    public Userinfo changeEmail(String username, String email) {
        if (!isEmail(email)) {
            throw new IllegalArgumentException("Not an email address: " + email);
        }
        Userinfo user = userinfoDao.findByUsername(username);
        if (emailExists(email)) {
            String message = String.format("Already existing email for '%s', cannot change email", email);
            LOG.error(message);
            throw new IllegalStateException(message);
        }
        user.setEmail(email);
        return userinfoDao.save(user);
    }

    public Userinfo changeName(String username, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name must not be empty");
        }
        Userinfo user = userinfoDao.findByUsername(username);
        if (user == null) {
            throw new NoSuchElementException(String.format("No console user is registered as '%s'", username));
        }
        user.setName(name);
        return userinfoDao.save(user);
    }

    /**
     * Sets a user up as owner of an affiliate campsite, including groups and roles. <b>All other scope groups are
     * removed</b>; existing roles stay. May run again for an owner set up before: an existing affiliate group keeps
     * its parent (its access mode). A new affiliate group starts as HOUSE CARDS ONLY.
     */
    public Userinfo setupAffiliateOwner(final Userinfo userinfo) {
        Market market = determineMarket(userinfo.getUsername());
        if (market == null) {
            throw new IllegalStateException("Cannot set up affiliate owner without market: " + userinfo.getUsername());
        }
        var marketGroups = groupService.getExtensionByMarket(market);
        Group selfServiceGroup = marketGroups.findSelfServiceStaffGroup();
        Group houseCardsOnly = marketGroups.findHouseCardsOnlySiteGroup();
        if (selfServiceGroup == null || houseCardsOnly == null) {
            throw new IllegalStateException(String.format(
                    "Self service group and/or %s group of market %s could not be found", AccessMode.HOUSE_CARDS_ONLY,
                    market));
        }

        Group affiliateGroup = affiliateGroupService.findAffiliateSiteGroup(userinfo.getUsername());
        if (affiliateGroup == null) {
            // default setup is house cards only
            affiliateGroup = affiliateGroupService.createAffiliateSiteGroup(userinfo.getUsername(), userinfo.getName(),
                    market, AccessMode.HOUSE_CARDS_ONLY);
        }
        List<Group> groups = new ArrayList<>(); // an affiliate owner only has his one SITE group
        groups.add(affiliateGroup);
        userinfo.setGroups(groups);

        userinfo.setGroup(selfServiceGroup);

        // add the roles if not already set
        if (!userinfo.containsRole(UserroleName.AFFILIATE_OWNER)) {
            userinfo.getRoles().add(userroleDao.findByName(UserroleName.AFFILIATE_OWNER.name()));
        }
        if (Market.HR != market && !userinfo.containsRole(UserroleName.AFFILIATE_PRICE_REPORTING)) {
            userinfo.getRoles().add(userroleDao.findByName(UserroleName.AFFILIATE_PRICE_REPORTING.name()));
        }
        return userinfoDao.save(userinfo);
    }

    /**
     * Checks the group set-up of an affiliate owner (before commissioning pedestals for him).
     *
     * @return the owner, or null when the set-up was not done yet (no affiliate group at all, a valid state)
     * @throws AffiliateSetupException when the set-up is broken
     */
    public Userinfo checkAffiliateOwnerSetup(String ownerUsername) {
        Userinfo owner = userinfoDao.findByUsername(ownerUsername);
        if (owner == null) {
            String message = String.format("Affiliate owner '%s' not found", ownerUsername);
            LOG.error(message);
            throw new AffiliateSetupException(message);
        }

        Group affiliateGroup = affiliateGroupService.findAffiliateSiteGroup(ownerUsername);
        Group ownersSiteGroup = owner.getGroup(GroupType.SITE);
        if (ownersSiteGroup == null && affiliateGroup == null) { // only this case is a valid state
            LOG.error("Seems that the affiliate owner's group setup is not done yet. There is no affiliate group.");
            return null;
        }
        if (ownersSiteGroup != null && affiliateGroup == null) {
            String message = "Seems something is wrong with the affiliate owner's group setup. The owner has a SITE "
                    + "group, but the naming is wrong ('" + ownersSiteGroup.getName() + "')";
            LOG.error(message);
            throw new AffiliateSetupException(message);
        }
        if (affiliateGroup.getParent() == null) {
            String message = "Seems something is wrong with the affiliate owner's group setup. The affiliate group has "
                    + "no parent group.";
            LOG.error(message);
            throw new AffiliateSetupException(message);
        }
        if (ownersSiteGroup == null) {
            String message = "Seems something is wrong with the affiliate owner's group setup. The affiliate group "
                    + "exists but is not associated to the owner.";
            LOG.error(message);
            throw new AffiliateSetupException(message);
        }
        if (!affiliateGroup.getId().equals(ownersSiteGroup.getId())) {
            String message = "Seems something is wrong with the affiliate owner's group setup. There is an affiliate "
                    + "group but it's not the one associated to the owner.";
            LOG.error(message);
            throw new AffiliateSetupException(message);
        }

        return owner;
    }

    /**
     * ERP partner number of the affiliate owner whose campsite the pedestal belongs to (for campsites settling
     * with the network). Walks the parent line of the pedestal's group from the pedestal up; the first group with
     * an associated user having a number wins.
     *
     * @return the number, or null when there is none or when two users of the same group have one (logged as a
     * configuration error; no number is better than the wrong one)
     */
    public String findSettlementPartnerNo(Pedestal pedestal) {
        String settlementPartnerNo = null;
        if (null != pedestal.getGroup()) {
            List<Long> siteParentLine = groupService.fetchParentLineIds(pedestal.getGroup().getId(), true);
            ListIterator<Long> siteParentLineIterator = siteParentLine.listIterator();
            while (siteParentLineIterator.hasNext() && settlementPartnerNo == null) {
                final Long groupId = siteParentLineIterator.next();
                final Group siteGroup = groupService.fetchByGroupId(groupId);
                for (Userinfo userinfo : findByAssociatedGroup(siteGroup)) {
                    if (userinfo.getSettlementPartnerNo() == null) {
                        continue;
                    }
                    if (settlementPartnerNo == null) {
                        settlementPartnerNo = userinfo.getSettlementPartnerNo();
                    } else if (!settlementPartnerNo.equals(userinfo.getSettlementPartnerNo())) {
                        // should not occur, but can with a wrong configuration
                        LOG.error(String.format("Too many settlement partner numbers for pedestal %s in group %s: "
                                + "%s and %s", pedestal, siteGroup.getName(), settlementPartnerNo,
                                userinfo.getSettlementPartnerNo()));
                        return null;
                    }
                }
            }
        }
        return settlementPartnerNo;
    }

    public List<Userinfo> findByAssociatedGroup(Group searchGroup) {
        return userinfoDao.findByAssociatedGroup(searchGroup);
    }

    /**
     * Users with one of the roles in the STAFF subtree of the principal.
     */
    public List<Userinfo> findByRoles(Userinfo principal, List<Userrole> roles) {
        Objects.requireNonNull(principal);
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("roles must not be empty");
        }
        List<Userinfo> users = new ArrayList<>();
        Group staffGroup = principal.getGroup(GroupType.STAFF);
        if (staffGroup == null) {
            return users;
        }
        List<Group> groups = groupService.findManageableGroups(staffGroup);
        if (!groups.isEmpty()) {
            users = userinfoDao.findByGroupsAndUserroles(groups, roles);
        }
        return users;
    }

    public List<Userinfo> findSelfServiceUsers(String username) {
        Userinfo userinfo = findByUsername(username);
        return findByRoles(userinfo, userroleDao.findAllSelfServiceRoles());
    }

    public Userinfo save(Userinfo userinfo) {
        return userinfoDao.save(userinfo);
    }

    public String getLocalPartOfEmail(String string) {
        if (isEmail(string)) {
            return string.split("@")[0].toLowerCase(Locale.ROOT);
        }
        return null;
    }

    public boolean isEmail(String email) {
        return email != null && EMAIL.matcher(email).matches();
    }
}
