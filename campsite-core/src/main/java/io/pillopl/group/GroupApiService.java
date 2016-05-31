package io.pillopl.group;

import io.pillopl.api.GroupApi;
import io.pillopl.api.exception.AccessNotAllowedException;
import io.pillopl.api.exception.ConflictException;
import io.pillopl.api.exception.InvalidInputException;
import io.pillopl.api.exception.ObjectNotFoundException;
import io.pillopl.dto.group.AcceptanceAgreementDTO;
import io.pillopl.dto.group.GroupAttributesDTO;
import io.pillopl.dto.group.GroupDTO;
import io.pillopl.dto.group.GroupTreeNodeDTO;
import io.pillopl.group.attribute.CommunicationLogAttributeService;
import io.pillopl.group.attribute.ExportCampsiteName;
import io.pillopl.group.attribute.ExportCampsiteNameService;
import io.pillopl.group.attribute.IssuerTenantService;
import io.pillopl.group.attribute.LocalListAttributeService;
import io.pillopl.group.attribute.SiteTenantService;
import io.pillopl.group.attribute.SupplierMarkerService;
import io.pillopl.group.attribute.WelcomeCardAdoptionService;
import io.pillopl.persistence.dao.GroupDao;
import io.pillopl.user.PrincipalResolver;
import io.pillopl.user.Userinfo;
import io.pillopl.user.UserrightName;
import io.pillopl.util.ApiExceptionTranslator;
import io.pillopl.util.ApiValues;
import io.pillopl.value.GroupId;
import io.pillopl.value.GroupName;
import io.pillopl.value.Username;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Organisation calls of the console on top of the group services. Every group named in a call must lie in the
 * calling user's scope of its tree; changes need the right CONFIG_EDIT.
 */
@Service
@Transactional
public class GroupApiService implements GroupApi {

    private static final Logger LOG = LoggerFactory.getLogger(GroupApiService.class);

    private final GroupService groupService;

    private final GroupManager groupManager;

    private final GroupDao groupDao;

    private final AffiliateGroupService affiliateGroupService;

    private final SiteTenantService siteTenantService;

    private final IssuerTenantService issuerTenantService;

    private final LocalListAttributeService localListAttributeService;

    private final CommunicationLogAttributeService communicationLogAttributeService;

    private final WelcomeCardAdoptionService welcomeCardAdoptionService;

    private final ExportCampsiteNameService exportCampsiteNameService;

    private final SupplierMarkerService supplierMarkerService;

    private final PrincipalResolver principalResolver;

    public GroupApiService(GroupService groupService, GroupManager groupManager, GroupDao groupDao,
                           AffiliateGroupService affiliateGroupService, SiteTenantService siteTenantService,
                           IssuerTenantService issuerTenantService,
                           LocalListAttributeService localListAttributeService,
                           CommunicationLogAttributeService communicationLogAttributeService,
                           WelcomeCardAdoptionService welcomeCardAdoptionService,
                           ExportCampsiteNameService exportCampsiteNameService,
                           SupplierMarkerService supplierMarkerService, PrincipalResolver principalResolver) {
        this.groupService = groupService;
        this.groupManager = groupManager;
        this.groupDao = groupDao;
        this.affiliateGroupService = affiliateGroupService;
        this.siteTenantService = siteTenantService;
        this.issuerTenantService = issuerTenantService;
        this.localListAttributeService = localListAttributeService;
        this.communicationLogAttributeService = communicationLogAttributeService;
        this.welcomeCardAdoptionService = welcomeCardAdoptionService;
        this.exportCampsiteNameService = exportCampsiteNameService;
        this.supplierMarkerService = supplierMarkerService;
        this.principalResolver = principalResolver;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupDTO> findManageableGroups(Username authUsername, String groupType, String search) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername);
            GroupType type = ApiValues.parseEnum(GroupType.class, groupType, "group type");
            Group scope = principal.getGroup(type);
            if (scope == null) {
                LOG.info(String.format("User '%s' has no %s scope", authUsername, type));
                return new ArrayList<>();
            }
            Set<Long> matching = new HashSet<>(GroupServiceHelper.collectSubtreeIds(scope, search));
            List<Group> result = new ArrayList<>();
            for (Group group : GroupServiceHelper.collectSubtree(scope)) {
                if (matching.contains(group.getId())) {
                    result.add(group);
                }
            }
            return GroupMapper.toDTOs(result);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public GroupTreeNodeDTO getGroupTree(Username authUsername, String groupType) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername);
            GroupType type = ApiValues.parseEnum(GroupType.class, groupType, "group type");
            Group scope = principal.getGroup(type);
            if (scope == null) {
                throw new ObjectNotFoundException(String.format("User '%s' has no %s scope", authUsername, type));
            }
            return GroupTreeFactory.createTree(scope);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public GroupDTO createGroup(Username authUsername, GroupId parentGroupId, GroupName name, String displayName) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.CONFIG_EDIT);
            Group parent = fetchManageable(principal, parentGroupId);
            if (groupDao.findByNameAndType(name.value(), parent.getType()) != null) {
                throw new ConflictException(String.format("%s group '%s' already exists", parent.getType(), name));
            }
            String display = displayName != null && !displayName.isBlank() ? displayName.trim() : name.value();
            Group child = groupManager.createChildGroup(parent, name.value(), display);
            LOG.info(String.format("User '%s' created group %s", authUsername,
                    GroupServiceHelper.groupToPrettyString(child)));
            return GroupMapper.toDTO(child);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AcceptanceAgreementDTO findAcceptanceAgreements(Username authUsername, GroupId issuerGroupId) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername);
            Group issuer = fetchManageable(principal, issuerGroupId);
            checkType(issuer, GroupType.ISSUER);
            return toAgreementDTO(issuer);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public AcceptanceAgreementDTO addAcceptanceAgreement(Username authUsername, GroupId issuerGroupId,
                                                         GroupId siteGroupId) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.CONFIG_EDIT);
            Group issuer = fetchManageable(principal, issuerGroupId);
            Group site = fetchManageable(principal, siteGroupId);
            checkType(issuer, GroupType.ISSUER);
            checkType(site, GroupType.SITE);
            return toAgreementDTO(groupManager.addAcceptanceAgreement(issuer, site));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public AcceptanceAgreementDTO removeAcceptanceAgreement(Username authUsername, GroupId issuerGroupId,
                                                            GroupId siteGroupId) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.CONFIG_EDIT);
            Group issuer = fetchManageable(principal, issuerGroupId);
            Group site = fetchManageable(principal, siteGroupId);
            checkType(issuer, GroupType.ISSUER);
            if (!issuer.getGroups().contains(site)) {
                throw new ObjectNotFoundException(String.format("No agreement of group %s with group %s",
                        issuerGroupId, siteGroupId));
            }
            return toAgreementDTO(groupManager.removeAcceptanceAgreement(issuer, site));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public String switchAccessMode(Username authUsername, GroupId affiliateGroupId, String accessMode,
                                   boolean listedInApp) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.CONFIG_EDIT);
            Group group = fetchManageable(principal, affiliateGroupId);
            AccessMode targetMode = ApiValues.parseEnum(AccessMode.class, accessMode, "access mode");
            if (!AffiliateGroupService.isAffiliate(group)) {
                throw new ConflictException("Not an affiliate campsite group: " + group.getName());
            }
            AccessMode result = affiliateGroupService.switchAccessMode(group.getId(), targetMode, listedInApp);
            if (result == null) {
                throw new ConflictException("Access mode of group " + group.getName() + " could not be switched");
            }
            return result.name();
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkGroupValidity(Username authUsername) {
        try {
            principalResolver.resolve(authUsername, UserrightName.CONFIG_EDIT);
            return groupService.validateGroupTree();
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public GroupAttributesDTO getGroupAttributes(Username authUsername, GroupId groupId) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername);
            Group group = fetchManageable(principal, groupId);
            Market market = groupService.determineMarketByGroupId(group.getId());
            String siteTenant = GroupType.SITE.isGroupType(group) ? siteTenantService.findSiteTenantNameFor(group)
                    : null;
            String issuerTenant = GroupType.ISSUER.isGroupType(group)
                    ? issuerTenantService.findIssuerTenantNameFor(group) : null;
            ExportCampsiteName exportName = exportCampsiteNameService.getExportCampsiteName(group.getId());
            AccessMode accessMode = AffiliateGroupService.isAffiliate(group)
                    ? AffiliateGroupService.checkAccessMode(group) : null;
            return new GroupAttributesDTO(group.getId(), group.getType(), market != null ? market.name() : null,
                    siteTenant, issuerTenant, localListAttributeService.isLocalListGroup(group),
                    communicationLogAttributeService.isGroupLoggable(group.getId()),
                    welcomeCardAdoptionService.adoptionIsAllowedFor(group),
                    ExportCampsiteName.computeName(exportName, group, null),
                    supplierMarkerService.isInSupplierStock(group), accessMode != null ? accessMode.name() : null);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    /**
     * The group must exist and lie in the principal's scope of its tree.
     */
    private Group fetchManageable(Userinfo principal, GroupId groupId) {
        Group group = groupService.fetchByGroupId(groupId.value());
        if (group == null) {
            throw ObjectNotFoundException.of("Group", groupId);
        }
        Group scope = principal.getGroup(group.getType());
        if (!groupService.isManageable(scope, group)) {
            throw new AccessNotAllowedException(String.format("User '%s' has no access to group %s",
                    principal.getUsername(), groupId));
        }
        return group;
    }

    private static void checkType(Group group, GroupType type) {
        if (!type.isGroupType(group)) {
            throw new InvalidInputException(String.format("Group %s is no %s group", group.getId(), type));
        }
    }

    private static AcceptanceAgreementDTO toAgreementDTO(Group issuer) {
        List<Group> agreements = GroupServiceHelper.getAcceptanceAgreements(issuer, GroupType.SITE);
        boolean own = false;
        for (Group associated : issuer.getGroups()) {
            if (GroupType.SITE.isGroupType(associated)) {
                own = true;
                break;
            }
        }
        return new AcceptanceAgreementDTO(issuer.getId(), !own && !agreements.isEmpty(),
                GroupMapper.toDTOs(agreements), GroupServiceHelper.extractAllAcceptingSiteGroupIds(issuer));
    }
}
