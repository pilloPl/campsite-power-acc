package io.pillopl.user;

import io.pillopl.group.AccessDeniedException;
import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.GroupType;
import io.pillopl.group.IsLinkedToGroup;
import io.pillopl.group.Market;
import io.pillopl.persistence.dao.UserinfoDao;
import io.pillopl.persistence.dao.UserroleDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

    public UserinfoService(UserinfoDao userinfoDao, UserroleDao userroleDao, GroupService groupService) {
        this.userinfoDao = userinfoDao;
        this.userroleDao = userroleDao;
        this.groupService = groupService;
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
        user.setName(name);
        return userinfoDao.save(user);
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
