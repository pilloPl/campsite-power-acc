package io.pillopl.webservice.console.group;

import io.pillopl.api.GroupApi;
import io.pillopl.value.GroupId;
import io.pillopl.value.GroupName;
import io.pillopl.webservice.console.common.AbstractWebService;
import io.pillopl.webservice.console.common.WebServicePaths;
import io.pillopl.webservice.console.common.WsErrorCode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Web service of the organisation tab: group trees, acceptance agreements, affiliate access mode, attributes.
 */
@RestController
@RequestMapping(WebServicePaths.GROUP)
public class GroupWebService extends AbstractWebService<GroupApi, GroupWebServiceFault> {

    public GroupWebService(GroupApi groupApi) {
        super(groupApi);
    }

    @PostMapping("/findManageableGroups")
    public FindManageableGroupsResponse findManageableGroups(@RequestBody FindManageableGroupsRequest request)
            throws GroupWebServiceFault {
        return invoke("findManageableGroups", () -> new FindManageableGroupsResponse(getBean().findManageableGroups(
                caller(), request.groupType(), request.search())));
    }

    @PostMapping("/getGroupTree")
    public GetGroupTreeResponse getGroupTree(@RequestBody GetGroupTreeRequest request) throws GroupWebServiceFault {
        return invoke("getGroupTree", () -> new GetGroupTreeResponse(getBean().getGroupTree(
                caller(), request.groupType())));
    }

    @PostMapping("/createGroup")
    public CreateGroupResponse createGroup(@RequestBody CreateGroupRequest request) throws GroupWebServiceFault {
        return invoke("createGroup", () -> new CreateGroupResponse(getBean().createGroup(
                caller(), GroupId.of(request.parentGroupId()),
                GroupName.of(request.name()), request.displayName())));
    }

    @PostMapping("/findAcceptanceAgreements")
    public FindAcceptanceAgreementsResponse findAcceptanceAgreements(
            @RequestBody FindAcceptanceAgreementsRequest request) throws GroupWebServiceFault {
        return invoke("findAcceptanceAgreements", () -> new FindAcceptanceAgreementsResponse(
                getBean().findAcceptanceAgreements(caller(),
                        GroupId.of(request.issuerGroupId()))));
    }

    @PostMapping("/addAcceptanceAgreement")
    public AddAcceptanceAgreementResponse addAcceptanceAgreement(@RequestBody AddAcceptanceAgreementRequest request)
            throws GroupWebServiceFault {
        return invoke("addAcceptanceAgreement", () -> new AddAcceptanceAgreementResponse(
                getBean().addAcceptanceAgreement(caller(),
                        GroupId.of(request.issuerGroupId()), GroupId.of(request.siteGroupId()))));
    }

    @PostMapping("/removeAcceptanceAgreement")
    public RemoveAcceptanceAgreementResponse removeAcceptanceAgreement(
            @RequestBody RemoveAcceptanceAgreementRequest request) throws GroupWebServiceFault {
        return invoke("removeAcceptanceAgreement", () -> new RemoveAcceptanceAgreementResponse(
                getBean().removeAcceptanceAgreement(caller(),
                        GroupId.of(request.issuerGroupId()), GroupId.of(request.siteGroupId()))));
    }

    @PostMapping("/switchAccessMode")
    public SwitchAccessModeResponse switchAccessMode(@RequestBody SwitchAccessModeRequest request)
            throws GroupWebServiceFault {
        return invoke("switchAccessMode", () -> {
            GroupId groupId = GroupId.of(request.affiliateGroupId());
            String mode = getBean().switchAccessMode(caller(), groupId,
                    request.accessMode(), Boolean.TRUE.equals(request.listedInApp()));
            return new SwitchAccessModeResponse(groupId.value(), mode);
        });
    }

    @PostMapping("/checkGroupValidity")
    public CheckGroupValidityResponse checkGroupValidity(@RequestBody CheckGroupValidityRequest request)
            throws GroupWebServiceFault {
        return invoke("checkGroupValidity", () -> new CheckGroupValidityResponse(
                getBean().checkGroupValidity(caller())));
    }

    @PostMapping("/getGroupAttributes")
    public GetGroupAttributesResponse getGroupAttributes(@RequestBody GetGroupAttributesRequest request)
            throws GroupWebServiceFault {
        return invoke("getGroupAttributes", () -> new GetGroupAttributesResponse(getBean().getGroupAttributes(
                caller(), GroupId.of(request.groupId()))));
    }

    @Override
    protected GroupWebServiceFault createFault(String message, WsErrorCode errorCode) {
        return new GroupWebServiceFault(message, errorCode);
    }
}
