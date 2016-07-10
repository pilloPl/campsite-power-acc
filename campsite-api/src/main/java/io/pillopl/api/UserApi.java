package io.pillopl.api;

import io.pillopl.commons.ListPage;
import io.pillopl.commons.Pagination;
import io.pillopl.dto.user.CurrentUserDTO;
import io.pillopl.dto.user.UserinfoDTO;
import io.pillopl.dto.user.UserinfoSummaryDTO;
import io.pillopl.value.EmailAddress;
import io.pillopl.value.Username;

import java.util.List;

/**
 * Console users in the STAFF scope of the calling user.
 */
public interface UserApi {

    UserinfoDTO findUser(Username authUsername, Username username);

    /**
     * @param market null or UNKNOWN for the market of the calling user
     */
    UserinfoDTO createUser(Username authUsername, EmailAddress email, String name, String market);

    /**
     * @param email null keeps the email
     * @param name  null keeps the name
     */
    UserinfoDTO updateUser(Username authUsername, Username username, EmailAddress email, String name);

    UserinfoDTO setupAffiliateOwner(Username authUsername, Username ownerUsername);

    ListPage<UserinfoSummaryDTO> findSelfServiceUsers(Username authUsername, Boolean affiliateOwner,
                                                      Boolean seasonalResident, Pagination pagination);

    List<UserinfoDTO> findUsersByRoles(Username authUsername, List<String> roleNames);

    /**
     * Console login: checks the password of an active user. Failed attempts are counted and audited; the user is
     * locked after MaxFailedLogins of them.
     *
     * @throws io.pillopl.api.exception.AccessNotAllowedException for an unknown, inactive or locked user or a wrong
     *                                                            password (one message for all of them)
     */
    CurrentUserDTO authenticate(Username username, String password);

    /**
     * The logged-in user with its effective roles and rights.
     */
    CurrentUserDTO getCurrentUser(Username authUsername);

    /**
     * Users in the STAFF scope of the caller (USER_ADMIN), optionally matching a search text.
     */
    List<UserinfoDTO> findUsers(Username authUsername, String search);

    /**
     * Sets a password (USER_ADMIN for other users; everybody for itself). An invited user becomes active.
     */
    UserinfoDTO setPassword(Username authUsername, Username username, String password);

    /**
     * @param status ACTIVE, LOCKED or DEACTIVATED
     */
    UserinfoDTO changeUserStatus(Username authUsername, Username username, String status);

    /**
     * Replaces the direct roles of a user; only roles the caller holds can be given.
     */
    UserinfoDTO replaceUserRoles(Username authUsername, Username username, List<String> roleNames);
}
