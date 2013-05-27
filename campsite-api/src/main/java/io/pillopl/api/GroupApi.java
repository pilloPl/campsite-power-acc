package io.pillopl.api;

import io.pillopl.dto.group.AcceptanceAgreementDTO;
import io.pillopl.dto.group.GroupAttributesDTO;
import io.pillopl.dto.group.GroupDTO;
import io.pillopl.dto.group.GroupTreeNodeDTO;
import io.pillopl.value.GroupId;
import io.pillopl.value.GroupName;
import io.pillopl.value.Username;

import java.util.List;

/**
 * Organisation: the three group trees, acceptance agreements, affiliate access mode and effective attributes. Every
 * call is restricted to the scope (subtree) of the calling user in the tree concerned.
 */
public interface GroupApi {

    List<GroupDTO> findManageableGroups(Username authUsername, String groupType, String search);

    GroupTreeNodeDTO getGroupTree(Username authUsername, String groupType);

    GroupDTO createGroup(Username authUsername, GroupId parentGroupId, GroupName name, String displayName);

    AcceptanceAgreementDTO findAcceptanceAgreements(Username authUsername, GroupId issuerGroupId);

    AcceptanceAgreementDTO addAcceptanceAgreement(Username authUsername, GroupId issuerGroupId, GroupId siteGroupId);

    AcceptanceAgreementDTO removeAcceptanceAgreement(Username authUsername, GroupId issuerGroupId,
                                                     GroupId siteGroupId);

    /**
     * @return the access mode after the switch
     */
    String switchAccessMode(Username authUsername, GroupId affiliateGroupId, String accessMode, boolean listedInApp);

    /**
     * @return true when all users, pedestals and groups hang in the right trees
     */
    boolean checkGroupValidity(Username authUsername);

    GroupAttributesDTO getGroupAttributes(Username authUsername, GroupId groupId);
}
