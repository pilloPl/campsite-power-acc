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
        return invoke("findUser", () -> new FindUserResponse(getBean().findUser(Username.of(request.authUsername()),
                Username.of(request.username()))));
    }

    @PostMapping("/createUser")
    public CreateUserResponse createUser(@RequestBody CreateUserRequest request) throws UserWebServiceFault {
        return invoke("createUser", () -> new CreateUserResponse(getBean().createUser(
                Username.of(request.authUsername()), EmailAddress.of(request.email()), request.name(),
                request.market())));
    }

    @PostMapping("/updateUser")
    public UpdateUserResponse updateUser(@RequestBody UpdateUserRequest request) throws UserWebServiceFault {
        return invoke("updateUser", () -> new UpdateUserResponse(getBean().updateUser(
                Username.of(request.authUsername()), Username.of(request.username()),
                EmailAddress.ofNullable(request.email()),
                request.name() != null && !request.name().isBlank() ? request.name().trim() : null)));
    }

    @PostMapping("/setupAffiliateOwner")
    public SetupAffiliateOwnerResponse setupAffiliateOwner(@RequestBody SetupAffiliateOwnerRequest request)
            throws UserWebServiceFault {
        return invoke("setupAffiliateOwner", () -> new SetupAffiliateOwnerResponse(getBean().setupAffiliateOwner(
                Username.of(request.authUsername()), Username.of(request.ownerUsername()))));
    }

    @PostMapping("/findSelfServiceUsers")
    public FindSelfServiceUsersResponse findSelfServiceUsers(@RequestBody FindSelfServiceUsersRequest request)
            throws UserWebServiceFault {
        return invoke("findSelfServiceUsers", () -> {
            ListPage<UserinfoSummaryDTO> page = getBean().findSelfServiceUsers(Username.of(request.authUsername()),
                    request.affiliateOwner(), request.seasonalResident(),
                    Pagination.orFirstPage(request.page(), request.rowsPerPage()));
            return new FindSelfServiceUsersResponse(page.items(), page.total(), page.page(), page.rowsPerPage());
        });
    }

    @PostMapping("/findUsersByRoles")
    public FindUsersByRolesResponse findUsersByRoles(@RequestBody FindUsersByRolesRequest request)
            throws UserWebServiceFault {
        return invoke("findUsersByRoles", () -> new FindUsersByRolesResponse(getBean().findUsersByRoles(
                Username.of(request.authUsername()), request.roles())));
    }

    @Override
    protected UserWebServiceFault createFault(String message, WsErrorCode errorCode) {
        return new UserWebServiceFault(message, errorCode);
    }
}
