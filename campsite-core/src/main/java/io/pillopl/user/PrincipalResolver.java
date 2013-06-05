package io.pillopl.user;

import io.pillopl.api.exception.AccessNotAllowedException;
import io.pillopl.api.exception.ObjectNotFoundException;
import io.pillopl.persistence.dao.UserinfoDao;
import io.pillopl.value.Username;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Resolves the calling user of a service interface call (the auth username of the request) and checks its rights.
 */
@Component
public class PrincipalResolver {

    private static final Logger LOG = LoggerFactory.getLogger(PrincipalResolver.class);

    private final UserinfoDao userinfoDao;

    public PrincipalResolver(UserinfoDao userinfoDao) {
        this.userinfoDao = userinfoDao;
    }

    /**
     * @throws AccessNotAllowedException when the user is unknown, deleted or locked
     */
    public Userinfo resolve(Username authUsername) {
        Userinfo principal = userinfoDao.findByUsername(authUsername.value());
        if (principal == null || principal.isDeleted()) {
            LOG.info(String.format("Call by unknown user '%s' refused", authUsername));
            throw new AccessNotAllowedException("Unknown user: " + authUsername);
        }
        if (principal.getStatus() == UserinfoStatus.LOCKED) {
            LOG.info(String.format("Call by locked user '%s' refused", authUsername));
            throw new AccessNotAllowedException("User is locked: " + authUsername);
        }
        return principal;
    }

    /**
     * @throws AccessNotAllowedException when the user is unknown or has not the right (directly or through a security
     *                                    group)
     */
    public Userinfo resolve(Username authUsername, UserrightName right) {
        Userinfo principal = resolve(authUsername);
        if (!principal.hasRight(right.name())) {
            LOG.info(String.format("User '%s' lacks right %s", authUsername, right));
            throw new AccessNotAllowedException(String.format("User '%s' lacks right %s", authUsername, right));
        }
        return principal;
    }

    /**
     * Another user named in the call (not the caller).
     *
     * @throws ObjectNotFoundException when the user does not exist or is deleted
     */
    public Userinfo findUser(Username username) {
        Userinfo userinfo = userinfoDao.findByUsername(username.value());
        if (userinfo == null || userinfo.isDeleted()) {
            throw ObjectNotFoundException.of("User", username);
        }
        return userinfo;
    }
}
