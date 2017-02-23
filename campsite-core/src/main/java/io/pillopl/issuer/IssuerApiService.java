package io.pillopl.issuer;

import io.pillopl.api.IssuerApi;
import io.pillopl.api.exception.AccessNotAllowedException;
import io.pillopl.api.exception.ConflictException;
import io.pillopl.api.exception.InvalidInputException;
import io.pillopl.api.exception.ObjectNotFoundException;
import io.pillopl.card.CampingCard;
import io.pillopl.card.CampingCardService;
import io.pillopl.card.CardMapper;
import io.pillopl.commons.TextUtil;
import io.pillopl.dto.admin.IssuerPartnerDTO;
import io.pillopl.dto.admin.IssuerSyncStatusDTO;
import io.pillopl.dto.admin.WhitelistFilterDTO;
import io.pillopl.dto.card.CampingCardDTO;
import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.GroupType;
import io.pillopl.group.attribute.WhitelistFilter;
import io.pillopl.group.attribute.WhitelistFilterService;
import io.pillopl.group.attribute.WhitelistFilterType;
import io.pillopl.persistence.dao.IssuerPartnerDao;
import io.pillopl.persistence.dao.WhitelistFilterDao;
import io.pillopl.user.PrincipalResolver;
import io.pillopl.user.Userinfo;
import io.pillopl.user.UserrightName;
import io.pillopl.util.ApiExceptionTranslator;
import io.pillopl.util.ApiValues;
import io.pillopl.util.DateService;
import io.pillopl.value.Username;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Issuer exchange calls of the console: partners, their sync states and the whitelist filters (REPORT_VIEW), editing
 * partners and filters and the run (CONFIG_EDIT: network configuration), a card sent again (BILLING_EDIT).
 */
@Service
public class IssuerApiService implements IssuerApi {

    private static final Logger LOG = LoggerFactory.getLogger(IssuerApiService.class);

    private final IssuerExchangeService issuerExchangeService;

    private final IssuerPartnerDao issuerPartnerDao;

    private final PrincipalResolver principalResolver;

    private final TransactionTemplate readOnlyTransaction;

    private final GroupService groupService;

    private final CampingCardService campingCardService;

    private final DateService dateService;

    private final WhitelistFilterService whitelistFilterService;

    private final WhitelistFilterDao whitelistFilterDao;

    static final String NOT_RUN = "NOT_RUN";

    public IssuerApiService(IssuerExchangeService issuerExchangeService, IssuerPartnerDao issuerPartnerDao,
                            PrincipalResolver principalResolver, PlatformTransactionManager transactionManager,
                            GroupService groupService, CampingCardService campingCardService,
                            DateService dateService, WhitelistFilterService whitelistFilterService,
                            WhitelistFilterDao whitelistFilterDao) {
        this.whitelistFilterService = whitelistFilterService;
        this.whitelistFilterDao = whitelistFilterDao;
        this.groupService = groupService;
        this.campingCardService = campingCardService;
        this.dateService = dateService;
        this.issuerExchangeService = issuerExchangeService;
        this.issuerPartnerDao = issuerPartnerDao;
        this.principalResolver = principalResolver;
        this.readOnlyTransaction = new TransactionTemplate(transactionManager);
        this.readOnlyTransaction.setReadOnly(true);
    }

    @Override
    public List<IssuerSyncStatusDTO> runIssuerExchange(Username authUsername) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.CONFIG_EDIT);
            LOG.info(String.format("Issuer exchange started by %s", principal.getUsername()));
            issuerExchangeService.exchangeAll();
            // the exchange commits per partner and direction: no transaction around it, one to read the result
            return readOnlyTransaction.execute(status -> findSyncStatuses());
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<IssuerPartnerDTO> findIssuerPartners(Username authUsername) {
        try {
            principalResolver.resolve(authUsername, UserrightName.REPORT_VIEW);
            List<IssuerPartnerDTO> result = new ArrayList<>();
            for (IssuerPartner partner : issuerPartnerDao.findByDeletedFalseOrderByName()) {
                result.add(toDTO(partner));
            }
            return result;
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional
    public IssuerPartnerDTO saveIssuerPartner(Username authUsername, IssuerPartnerDTO dto) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.CONFIG_EDIT);
            if (dto == null) {
                throw new InvalidInputException("Partner is missing");
            }
            String name = TextUtil.trimToNull(dto.name());
            IssuerPartner sameName = name != null ? issuerPartnerDao.findByName(name) : null;
            IssuerPartner partner;
            if (dto.id() == null) {
                partner = new IssuerPartner();
                partner.setCreateDate(dateService.getNow());
            } else {
                partner = issuerPartnerDao.findById(dto.id()).filter(p -> !p.isDeleted())
                        .orElseThrow(() -> ObjectNotFoundException.of("Issuer partner", dto.id()));
            }
            if (sameName != null && !sameName.getId().equals(partner.getId())) {
                throw new ConflictException("Issuer partner " + name + " exists already");
            }
            partner.setName(name);
            partner.setDisplayName(TextUtil.trimToNull(dto.displayName()));
            partner.setIssuerGroup(group(dto.issuerGroupId(), "issuer group"));
            partner.setSiteGroup(dto.siteGroupId() != null ? group(dto.siteGroupId(), "site group") : null);
            partner.setInterfaceVersion(ApiValues.parseEnum(IssuerPartner.InterfaceVersion.class,
                    dto.interfaceVersion(), "interface version"));
            partner.setEndpoint(TextUtil.trimToNull(dto.endpoint()));
            partner.setCardImport(dto.cardImport());
            partner.setCardExport(dto.cardExport());
            partner.setRecordExport(dto.recordExport());
            partner.setRecordImport(dto.recordImport());
            partner.setActive(dto.active());
            partner.validate();
            partner = issuerPartnerDao.saveAndFlush(partner);
            LOG.info(String.format("Issuer partner %s saved by %s", partner, principal.getUsername()));
            return toDTO(partner);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<IssuerSyncStatusDTO> findIssuerSyncStatuses(Username authUsername) {
        try {
            principalResolver.resolve(authUsername, UserrightName.REPORT_VIEW);
            List<IssuerSyncStatusDTO> result = new ArrayList<>();
            for (IssuerPartner partner : issuerPartnerDao.findByDeletedFalseOrderByName()) {
                result.addAll(syncStatusMatrixRow(partner));
            }
            return result;
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public CampingCardDTO resyncCard(Username authUsername, Long cardId) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.BILLING_EDIT);
            CampingCard card = campingCardService.requestPartnerResync(principal, cardId);
            // logged only once the resync was accepted: a card kept out of the exchange throws before
            LOG.info(String.format("Card %s marked %s for the partners by %s", cardId, card.getPartnerSyncState(),
                    principal.getUsername()));
            return CardMapper.toDTO(card, dateService.getNow());
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<WhitelistFilterDTO> findWhitelistFilters(Username authUsername) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.REPORT_VIEW);
            List<WhitelistFilterDTO> result = new ArrayList<>();
            for (WhitelistFilter filter : whitelistFilterService.findFiltersOfSubtree(
                    principal.getGroup(GroupType.ISSUER))) {
                result.add(toDTO(filter));
            }
            return result;
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional
    public WhitelistFilterDTO saveWhitelistFilter(Username authUsername, WhitelistFilterDTO dto) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.CONFIG_EDIT);
            if (dto == null) {
                throw new InvalidInputException("Whitelist filter is missing");
            }
            Group issuerGroup = issuerGroupInScope(principal, dto.groupId());
            if (dto.id() != null) {
                WhitelistFilter existing = filterInScope(principal, dto.id());
                if (!existing.getGroupId().equals(issuerGroup.getId())) {
                    throw new InvalidInputException("A whitelist filter cannot move to another group");
                }
            }
            IssuerPartner partner = null;
            if (dto.issuerPartnerId() != null) {
                partner = issuerPartnerDao.findById(dto.issuerPartnerId()).filter(p -> !p.isDeleted())
                        .orElseThrow(() -> ObjectNotFoundException.of("Issuer partner", dto.issuerPartnerId()));
            }
            WhitelistFilterType type = dto.filterType() != null ? ApiValues.parseEnum(WhitelistFilterType.class,
                    dto.filterType(), "filter type") : WhitelistFilterType.IMPORT_EXPORT;
            WhitelistFilter saved = whitelistFilterService.saveFilter(issuerGroup, dto.synchronizable(), type,
                    partner);
            LOG.info(String.format("Whitelist filter %s saved by %s", saved, principal.getUsername()));
            return toDTO(saved);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional
    public void deleteWhitelistFilter(Username authUsername, Long filterId) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.CONFIG_EDIT);
            WhitelistFilter filter = filterInScope(principal, filterId);
            whitelistFilterService.deleteFilter(filter);
            LOG.info(String.format("Whitelist filter %s deleted by %s", filter, principal.getUsername()));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    private Group issuerGroupInScope(Userinfo principal, Long groupId) {
        Group group = group(groupId, "issuer group");
        if (!GroupType.ISSUER.isGroupType(group)) {
            throw new InvalidInputException(String.format("Group %s is no ISSUER group", groupId));
        }
        if (!groupService.isManageable(principal.getGroup(GroupType.ISSUER), group)) {
            throw new AccessNotAllowedException(String.format("User '%s' has no access to group %s",
                    principal.getUsername(), groupId));
        }
        return group;
    }

    private WhitelistFilter filterInScope(Userinfo principal, Long filterId) {
        WhitelistFilter filter = filterId != null ? whitelistFilterDao.findById(filterId).orElse(null) : null;
        if (filter == null || !groupService.isManageable(principal.getGroup(GroupType.ISSUER), filter.getGroup())) {
            throw ObjectNotFoundException.of("Whitelist filter", filterId);
        }
        return filter;
    }

    static WhitelistFilterDTO toDTO(WhitelistFilter f) {
        Group group = f.getGroup();
        IssuerPartner partner = f.getIssuerPartner();
        return new WhitelistFilterDTO(f.getId(), group.getId(), group.getName(), group.getDisplayName(),
                f.isSynchronizable(), f.getWhitelistFilterType().name(), partner != null ? partner.getId() : null,
                partner != null ? partner.getName() : null);
    }

    /**
     * The cells of a partner: one per switched-on direction, NOT_RUN where the direction never ran.
     */
    static List<IssuerSyncStatusDTO> syncStatusMatrixRow(IssuerPartner partner) {
        List<IssuerSyncStatusDTO> row = new ArrayList<>();
        for (IssuerSyncStatus.Direction direction : partner.getDirections()) {
            IssuerSyncStatus status = partner.getSyncStatuses().stream()
                    .filter(s -> s.getDirection() == direction).findFirst().orElse(null);
            row.add(status != null ? toDTO(partner, status)
                    : new IssuerSyncStatusDTO(partner.getName(), partner.getInterfaceVersion().name(),
                    direction.name(), NOT_RUN, null, null, 0, 0, null));
        }
        return row;
    }

    private Group group(Long groupId, String what) {
        if (groupId == null) {
            throw new InvalidInputException("The " + what + " is missing");
        }
        Group group = groupService.fetchByGroupId(groupId);
        if (group == null) {
            throw ObjectNotFoundException.of("Group", groupId);
        }
        return group;
    }

    static IssuerPartnerDTO toDTO(IssuerPartner p) {
        return new IssuerPartnerDTO(p.getId(), p.getName(), p.getDisplayName(), p.getIssuerGroup().getId(),
                p.getIssuerGroup().getName(), p.getSiteGroup() != null ? p.getSiteGroup().getId() : null,
                p.getSiteGroup() != null ? p.getSiteGroup().getName() : null, p.getInterfaceVersion().name(),
                p.getEndpoint(), p.isCardImport(), p.isCardExport(), p.isRecordExport(), p.isRecordImport(),
                p.isActive(), p.getCreateDate());
    }

    private List<IssuerSyncStatusDTO> findSyncStatuses() {
        List<IssuerSyncStatusDTO> result = new ArrayList<>();
        for (IssuerPartner partner : issuerPartnerDao.findExchanging()) {
            List<IssuerSyncStatus> statuses = new ArrayList<>(partner.getSyncStatuses());
            statuses.sort(Comparator.comparing(IssuerSyncStatus::getDirection));
            for (IssuerSyncStatus status : statuses) {
                result.add(toDTO(partner, status));
            }
        }
        return result;
    }

    static IssuerSyncStatusDTO toDTO(IssuerPartner partner, IssuerSyncStatus s) {
        return new IssuerSyncStatusDTO(partner.getName(), partner.getInterfaceVersion().name(),
                s.getDirection().name(), s.getStatus().name(), s.getLastRun(), s.getLastSuccess(),
                s.getItemsAccepted(), s.getItemsRejected(), s.getMessage());
    }
}
