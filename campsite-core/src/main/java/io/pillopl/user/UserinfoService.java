package io.pillopl.user;

import io.pillopl.group.AccessDeniedException;
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

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Console users: lookups, administrative scope over other users and guests, user set-up.
 */
@Service
@Transactional
public class UserinfoService {

    private static final Logger LOG = LoggerFactory.getLogger(UserinfoService.class);

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
