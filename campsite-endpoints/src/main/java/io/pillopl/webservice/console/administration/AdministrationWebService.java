package io.pillopl.webservice.console.administration;

import io.pillopl.api.AdministrationApi;
import io.pillopl.commons.DateRange;
import io.pillopl.commons.SortOrder;
import io.pillopl.dto.admin.AuditFilterDTO;
import io.pillopl.webservice.console.common.AbstractWebService;
import io.pillopl.webservice.console.common.WebServicePaths;
import io.pillopl.webservice.console.common.WsErrorCode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web service of the administration tab: audit log, system properties, rights matrix, user view refresh.
 */
@RestController
@RequestMapping(WebServicePaths.ADMINISTRATION)
public class AdministrationWebService extends AbstractWebService<AdministrationApi, AdministrationWebServiceFault> {

    public AdministrationWebService(AdministrationApi administrationApi) {
        super(administrationApi);
    }

    @PostMapping("/findAuditEntries")
    public FindAuditEntriesResponse findAuditEntries(@RequestBody FindAuditEntriesRequest request)
            throws AdministrationWebServiceFault {
        return invoke("findAuditEntries", () -> {
            AuditFilterDTO filter = new AuditFilterDTO(request.name(), request.type(), request.status(),
                    request.category(), DateRange.ofDays(request.dateFrom(), request.dateTo()), request.freeSearch());
            return new FindAuditEntriesResponse(getBean().findAuditEntries(caller(),
                    filter, SortOrder.of(request.sortColumn(), request.sortOrder())));
        });
    }

    @PostMapping("/getSystemProperties")
    public GetSystemPropertiesResponse getSystemProperties(@RequestBody GetSystemPropertiesRequest request)
            throws AdministrationWebServiceFault {
        return invoke("getSystemProperties", () -> new GetSystemPropertiesResponse(
                getBean().getSystemProperties(caller())));
    }

    @PostMapping("/setSystemProperty")
    public SetSystemPropertyResponse setSystemProperty(@RequestBody SetSystemPropertyRequest request)
            throws AdministrationWebServiceFault {
        return invoke("setSystemProperty", () -> new SetSystemPropertyResponse(getBean().setSystemProperty(
                caller(), request.key(), request.value(), request.description())));
    }

    @PostMapping("/findRoles")
    public FindRolesResponse findRoles(@RequestBody FindRolesRequest request) throws AdministrationWebServiceFault {
        return invoke("findRoles", () -> new FindRolesResponse(getBean().findRoles(
                caller())));
    }

    @PostMapping("/refreshUserSummary")
    public RefreshUserSummaryResponse refreshUserSummary(@RequestBody RefreshUserSummaryRequest request)
            throws AdministrationWebServiceFault {
        return invoke("refreshUserSummary", () -> {
            getBean().refreshUserSummary(caller());
            return new RefreshUserSummaryResponse(true);
        });
    }

    @Override
    protected AdministrationWebServiceFault createFault(String message, WsErrorCode errorCode) {
        return new AdministrationWebServiceFault(message, errorCode);
    }
}
