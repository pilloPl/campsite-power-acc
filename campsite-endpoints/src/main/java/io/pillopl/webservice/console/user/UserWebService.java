package io.pillopl.webservice.console.user;

import io.pillopl.api.UserApi;
import io.pillopl.commons.ListPage;
import io.pillopl.commons.Pagination;
import io.pillopl.dto.user.UserinfoSummaryDTO;
import io.pillopl.value.EmailAddress;
import io.pillopl.value.Username;
import io.pillopl.webservice.console.common.AbstractWebService;
import io.pillopl.webservice.console.common.WebServicePaths;
import io.pillopl.webservice.console.common.WsErrorCode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web service of the users of the console (administration tab and the user's own profile).
 */
@RestController
@RequestMapping(WebServicePaths.USER)
public class UserWebService extends AbstractWebService<UserApi, UserWebServiceFault> {

    public UserWebService(UserApi userApi) {
        super(userApi);
    }

    @PostMapping("/findUser")
    public FindUserResponse findUser(@RequestBody FindUserRequest request) throws UserWebServiceFault {
        return invoke("findUser", () -> new FindUserResponse(getBean().findUser(caller(),
                Username.of(request.username()))));
    }

    @PostMapping("/createUser")
    public CreateUserResponse createUser(@RequestBody CreateUserRequest request) throws UserWebServiceFault {
        return invoke("createUser", () -> new CreateUserResponse(getBean().createUser(
                caller(), EmailAddress.of(request.email()), request.name(),
                request.market())));
    }

    @PostMapping("/updateUser")
    public UpdateUserResponse updateUser(@RequestBody UpdateUserRequest request) throws UserWebServiceFault {
        return invoke("updateUser", () -> new UpdateUserResponse(getBean().updateUser(
                caller(), Username.of(request.username()),
                EmailAddress.ofNullable(request.email()),
                request.name() != null && !request.name().isBlank() ? request.name().trim() : null)));
    }

    @PostMapping("/setupAffiliateOwner")
    public SetupAffiliateOwnerResponse setupAffiliateOwner(@RequestBody SetupAffiliateOwnerRequest request)
            throws UserWebServiceFault {
        return invoke("setupAffiliateOwner", () -> new SetupAffiliateOwnerResponse(getBean().setupAffiliateOwner(
                caller(), Username.of(request.ownerUsername()))));
    }

    @PostMapping("/findSelfServiceUsers")
    public FindSelfServiceUsersResponse findSelfServiceUsers(@RequestBody FindSelfServiceUsersRequest request)
            throws UserWebServiceFault {
        return invoke("findSelfServiceUsers", () -> {
            ListPage<UserinfoSummaryDTO> page = getBean().findSelfServiceUsers(caller(),
                    request.affiliateOwner(), request.seasonalResident(),
                    Pagination.orFirstPage(request.page(), request.rowsPerPage()));
            return new FindSelfServiceUsersResponse(page.items(), page.total(), page.page(), page.rowsPerPage());
        });
    }

    @PostMapping("/findUsersByRoles")
    public FindUsersByRolesResponse findUsersByRoles(@RequestBody FindUsersByRolesRequest request)
            throws UserWebServiceFault {
        return invoke("findUsersByRoles", () -> new FindUsersByRolesResponse(getBean().findUsersByRoles(
                caller(), request.roles())));
    }

    @PostMapping("/findUsers")
    public FindUsersByRolesResponse findUsers(@RequestBody FindUsersRequest request) throws UserWebServiceFault {
        return invoke("findUsers", () -> new FindUsersByRolesResponse(getBean().findUsers(caller(),
                request.search())));
    }

    @PostMapping("/setPassword")
    public UpdateUserResponse setPassword(@RequestBody SetPasswordRequest request) throws UserWebServiceFault {
        return invoke("setPassword", () -> new UpdateUserResponse(getBean().setPassword(caller(),
                Username.of(request.username()), request.password())));
    }

    @PostMapping("/changeUserStatus")
    public UpdateUserResponse changeUserStatus(@RequestBody ChangeUserStatusRequest request)
            throws UserWebServiceFault {
        return invoke("changeUserStatus", () -> new UpdateUserResponse(getBean().changeUserStatus(caller(),
                Username.of(request.username()), request.status())));
    }

    @PostMapping("/replaceUserRoles")
    public UpdateUserResponse replaceUserRoles(@RequestBody ReplaceUserRolesRequest request)
            throws UserWebServiceFault {
        return invoke("replaceUserRoles", () -> new UpdateUserResponse(getBean().replaceUserRoles(caller(),
                Username.of(request.username()), request.roles())));
    }

    @Override
    protected UserWebServiceFault createFault(String message, WsErrorCode errorCode) {
        return new UserWebServiceFault(message, errorCode);
    }
}
