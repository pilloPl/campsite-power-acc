package io.pillopl.api;

import io.pillopl.commons.ListPage;
import io.pillopl.commons.Pagination;
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
}
