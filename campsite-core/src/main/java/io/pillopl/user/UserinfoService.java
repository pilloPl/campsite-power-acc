package io.pillopl.user;

import io.pillopl.group.AccessDeniedException;
import io.pillopl.group.AccessMode;
import io.pillopl.group.AffiliateGroupService;
import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.GroupType;
import io.pillopl.group.IsLinkedToGroup;
import io.pillopl.group.Market;
import io.pillopl.api.exception.ObjectNotFoundException;
import io.pillopl.config.SystemPropertyService;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.persistence.dao.UserinfoDao;
import io.pillopl.persistence.dao.UserroleDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    static final int MIN_PASSWORD_LENGTH = 8;

    private final UserinfoDao userinfoDao;

    private final UserroleDao userroleDao;

    private final GroupService groupService;

    private final AffiliateGroupService affiliateGroupService;

    private final PasswordEncoder passwordEncoder;

    private final SystemPropertyService systemPropertyService;

    public UserinfoService(UserinfoDao userinfoDao, UserroleDao userroleDao, GroupService groupService,
                           AffiliateGroupService affiliateGroupService, PasswordEncoder passwordEncoder,
                           SystemPropertyService systemPropertyService) {
        this.userinfoDao = userinfoDao;
        this.userroleDao = userroleDao;
        this.groupService = groupService;
        this.affiliateGroupService = affiliateGroupService;
        this.passwordEncoder = passwordEncoder;
        this.systemPropertyService = systemPropertyService;
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
            LOG.error(String.format("No console user is registered as '%s'", username));
            return null;
        }
        if (userinfo.getGroup() == null) {
            LOG.error(String.format("Console user '%s' belongs to no organisation group", username));
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
        throw new AccessDeniedException(String.format("Console user '%s' may not manage console user '%s'",
                user.getUsername(), userIsManageable.getUsername()));
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
     * New, not yet persisted user in status INVITED without a password (an administrator sets it), no groups and no
     * roles.
     */
    public Userinfo createUserinfo(String email, String name) {
        Userinfo userinfoToCreate = new Userinfo();
        userinfoToCreate.setStatus(UserinfoStatus.INVITED);
        userinfoToCreate.setEmail(email);
        userinfoToCreate.setName(name != null && !name.isBlank() ? name : DEFAULT_NAME);
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

        LOG.info("Looking for a console user with the same e-mail");
        if (emailExists(email)) {
            throw new IllegalStateException(String.format("The e-mail '%s' is taken by another console user", email));
        }

        LOG.info("No campsite network given, taking the one of the creating user");
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

        // the login name is derived only now, from the e-mail
        return saveWithDerivedLogin(userinfoToCreate);
    }

    /**
     * Saves a new user with the local part of its email as username. When taken, "-1" ... "-998" is appended; when
     * all are taken, the database id becomes the suffix.
     */
    public Userinfo saveWithDerivedLogin(Userinfo userinfo) {
        Objects.requireNonNull(userinfo);
        String email = userinfo.getEmail();
        if (userinfo.getUsername() != null) {
            throw new IllegalArgumentException("A new console user must come without a login name, it is derived from the e-mail");
        }
        if (!isEmail(email)) {
            throw new IllegalArgumentException(String.format("'%s' is no valid e-mail", email));
        }
        if (userinfo.getId() != null) {
            throw new IllegalArgumentException(String.format("Console user with e-mail %s is stored already", email));
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

        // too many namesakes: the database id makes the login name unique
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
            String message = String.format("The e-mail '%s' is taken, the e-mail stays unchanged", email);
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
        if (ownersSiteGroup == null && affiliateGroup == null) { // neither group set yet: the expected starting point
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
                for (Userinfo userinfo : findWithScopeGroup(siteGroup)) {
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

    public List<Userinfo> findWithScopeGroup(Group searchGroup) {
        return userinfoDao.findWithScopeGroup(searchGroup);
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
            users = userinfoDao.findInGroupsWithRoles(groups, roles);
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

    // ---------------------------------------------------------------- console login and user administration

    /**
     * A successful login resets the failed attempts and remembers the date.
     */
    public void recordSuccessfulLogin(String username) {
        Userinfo userinfo = userinfoDao.findByUsername(username);
        if (userinfo == null) {
            return;
        }
        userinfo.setLastLoginDate(LocalDateTime.now());
        userinfo.setFailedLogins(0);
    }

    /**
     * Counts a failed login of a known user; the user is locked when MaxFailedLogins is reached.
     *
     * @return true when this attempt locked the user
     */
    public boolean recordFailedLogin(String username) {
        Userinfo userinfo = userinfoDao.findByUsername(username);
        if (userinfo == null || userinfo.isDeleted()) {
            return false;
        }
        userinfo.setFailedLogins(userinfo.getFailedLogins() + 1);
        if (userinfo.getStatus() == UserinfoStatus.ACTIVE
                && userinfo.getFailedLogins() >= systemPropertyService.getMaxFailedLogins()) {
            LOG.warn(String.format("Console user '%s' locked after %d failed logins", username,
                    userinfo.getFailedLogins()));
            userinfo.setStatus(UserinfoStatus.LOCKED);
            return true;
        }
        return false;
    }

    /**
     * Sets the password of a user the principal manages (or of the principal itself). An invited user becomes
     * active with its first password.
     */
    public Userinfo setPassword(Userinfo principal, String username, String rawPassword) {
        Userinfo userinfo = findManageable(principal, username, true);
        if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(String.format("A password needs at least %d characters",
                    MIN_PASSWORD_LENGTH));
        }
        if (rawPassword.isBlank() || rawPassword.equalsIgnoreCase(userinfo.getUsername())) {
            throw new IllegalArgumentException("The password must not be the username");
        }
        userinfo.setPasswordHash(passwordEncoder.encode(rawPassword));
        if (userinfo.getStatus() == UserinfoStatus.INVITED) {
            userinfo.setStatus(UserinfoStatus.ACTIVE);
        }
        userinfo.setFailedLogins(0);
        return userinfo;
    }

    /**
     * Locks, unlocks or deactivates a user the principal manages. Nobody changes its own status; unlocking resets the
     * failed logins; an invited user stays invited until it has a password.
     */
    public Userinfo changeStatus(Userinfo principal, String username, UserinfoStatus status) {
        Objects.requireNonNull(status);
        Userinfo userinfo = findManageable(principal, username, false);
        if (status == UserinfoStatus.INVITED) {
            throw new IllegalArgumentException("A user cannot be set back to INVITED");
        }
        if (status == UserinfoStatus.ACTIVE && userinfo.getPasswordHash() == null) {
            throw new IllegalArgumentException("A user without a password cannot be activated");
        }
        userinfo.setStatus(status);
        if (status == UserinfoStatus.ACTIVE) {
            userinfo.setFailedLogins(0);
        }
        return userinfo;
    }

    /**
     * Replaces the direct roles of a user the principal manages. Only roles the principal holds itself can be given,
     * and nobody changes its own roles.
     */
    public Userinfo replaceRoles(Userinfo principal, String username, List<String> roleNames) {
        Userinfo userinfo = findManageable(principal, username, false);
        List<Userrole> roles = new ArrayList<>();
        for (String roleName : roleNames != null ? roleNames : List.<String>of()) {
            Userrole role = userroleDao.findByName(roleName);
            if (role == null) {
                throw new IllegalArgumentException("Unknown role " + roleName);
            }
            if (!principal.getEffectiveRoles().contains(role)) {
                throw new AccessDeniedException(String.format("Role %s cannot be given by %s", roleName,
                        principal.getUsername()));
            }
            roles.add(role);
        }
        userinfo.getRoles().clear();
        userinfo.getRoles().addAll(roles);
        return userinfo;
    }

    /**
     * Users whose own group lies in the STAFF scope of the principal, optionally filtered by a search text.
     */
    @Transactional(readOnly = true)
    public List<Userinfo> findManageableUsers(Userinfo principal, String search) {
        Group staffGroup = principal.getGroup(GroupType.STAFF);
        if (staffGroup == null) {
            return List.of();
        }
        List<Group> groups = groupService.findManageableGroups(staffGroup);
        if (groups.isEmpty()) {
            return List.of();
        }
        return userinfoDao.findInGroups(groups, search == null || search.isBlank() ? null : search.trim());
    }

    private Userinfo findManageable(Userinfo principal, String username, boolean introspection) {
        Userinfo userinfo = userinfoDao.findByUsername(username);
        if (userinfo == null || userinfo.isDeleted()) {
            throw ObjectNotFoundException.of("User", username);
        }
        if (!introspection && principal.getUsername().equals(userinfo.getUsername())) {
            throw new AccessDeniedException("Users do not change their own status or roles");
        }
        checkIsManageable(principal, userinfo, introspection);
        return userinfo;
    }
}
