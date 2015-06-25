package io.pillopl.infrastructure;

import io.pillopl.api.CommissioningApi;
import io.pillopl.api.exception.ConflictException;
import io.pillopl.api.exception.InvalidInputException;
import io.pillopl.api.exception.ObjectNotFoundException;
import io.pillopl.audit.AuditActionType;
import io.pillopl.audit.AuditService;
import io.pillopl.dto.infrastructure.CommissioningDTO;
import io.pillopl.dto.infrastructure.LocationDTO;
import io.pillopl.dto.infrastructure.PedestalImportDTO;
import io.pillopl.dto.infrastructure.SupplierStockEntryDTO;
import io.pillopl.group.AccessMode;
import io.pillopl.infrastructure.supplier.SupplierPedestalImportService;
import io.pillopl.user.PrincipalResolver;
import io.pillopl.user.Userinfo;
import io.pillopl.user.UserinfoService;
import io.pillopl.user.UserrightName;
import io.pillopl.util.ApiExceptionTranslator;
import io.pillopl.util.ApiValues;
import io.pillopl.value.InstallerToken;
import io.pillopl.value.PedestalCode;
import io.pillopl.value.PedestalId;
import io.pillopl.value.Username;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Commissioning calls of the console and of the installers' app on top of {@link CommissioningService},
 * {@link AffiliatePedestalService} and the supplier import. Preparing and performing commissionings and imports need
 * PEDESTAL_COMMISSION; decommissioning is done by the owner of the campsite within its SITE scope.
 */
@Service
@Transactional
public class CommissioningApiService implements CommissioningApi {

    private static final Logger LOG = LoggerFactory.getLogger(CommissioningApiService.class);

    /**
     * Larger deliveries come as file through the supplier import job.
     */
    static final int MAX_PEDESTALS_PER_IMPORT_CALL = 200;

    private final CommissioningService commissioningService;

    private final AffiliatePedestalService affiliatePedestalService;

    private final SupplierPedestalImportService supplierPedestalImportService;

    private final PedestalService pedestalService;

    private final UserinfoService userinfoService;

    private final AuditService auditService;

    private final PrincipalResolver principalResolver;

    public CommissioningApiService(CommissioningService commissioningService,
                                   AffiliatePedestalService affiliatePedestalService,
                                   SupplierPedestalImportService supplierPedestalImportService,
                                   PedestalService pedestalService, UserinfoService userinfoService,
                                   AuditService auditService, PrincipalResolver principalResolver) {
        this.commissioningService = commissioningService;
        this.affiliatePedestalService = affiliatePedestalService;
        this.supplierPedestalImportService = supplierPedestalImportService;
        this.pedestalService = pedestalService;
        this.userinfoService = userinfoService;
        this.auditService = auditService;
        this.principalResolver = principalResolver;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommissioningDTO> findCommissionings(Username authUsername) {
        try {
            principalResolver.resolve(authUsername, UserrightName.PEDESTAL_COMMISSION);
            return CommissioningMapper.toDTOs(commissioningService.findAll(authUsername.value()));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CommissioningDTO findCommissioningByToken(Username installerUsername, InstallerToken token) {
        try {
            principalResolver.resolve(installerUsername, UserrightName.PEDESTAL_COMMISSION);
            Commissioning commissioning = commissioningService.findByToken(installerUsername.value(), token.value());
            if (commissioning == null) {
                throw ObjectNotFoundException.of("Commissioning", token);
            }
            return CommissioningMapper.toDTO(commissioning);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public CommissioningDTO createCommissioning(Username authUsername, Username ownerUsername, LocationDTO location,
                                                String comments, boolean listedInApp, String accessMode) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.PEDESTAL_COMMISSION);
            principalResolver.findUser(ownerUsername);
            Userinfo owner = userinfoService.checkAffiliateOwnerSetup(ownerUsername.value());
            if (owner == null) {
                throw new ConflictException("Affiliate owner is not set up yet: " + ownerUsername);
            }
            userinfoService.checkIsManageable(principal, owner, false);

            AccessMode mode = ApiValues.parseEnumOrNull(AccessMode.class, accessMode, "access mode");
            Commissioning commissioning = new Commissioning();
            commissioning.setAffiliateOwner(owner);
            commissioning.setLocation(PedestalMapper.toLocation(location));
            commissioning.setComments(comments);
            commissioning.setListedInApp(listedInApp);
            commissioning.setAccessMode(mode != null ? mode : AccessMode.HOUSE_CARDS_ONLY);
            return CommissioningMapper.toDTO(commissioningService.create(commissioning));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierStockEntryDTO> getSupplierStock(Username authUsername) {
        try {
            principalResolver.resolve(authUsername, UserrightName.PEDESTAL_COMMISSION);
            List<SupplierStockEntryDTO> result = new ArrayList<>();
            for (SupplierStockEntry entry : affiliatePedestalService.supplierStock(authUsername.value())) {
                result.add(PedestalMapper.toDTO(entry));
            }
            return result;
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public List<Long> commissionPedestals(Username installerUsername, InstallerToken token,
                                          List<PedestalId> pedestalIds) {
        try {
            principalResolver.resolve(installerUsername, UserrightName.PEDESTAL_COMMISSION);
            if (pedestalIds == null || pedestalIds.isEmpty()) {
                throw new InvalidInputException("No pedestals to commission");
            }
            List<Long> failed = new ArrayList<>();
            List<Pedestal> pedestals = new ArrayList<>();
            for (PedestalId pedestalId : pedestalIds) {
                Pedestal pedestal = pedestalService.findActive(pedestalId.value());
                if (pedestal == null) {
                    LOG.warn("Pedestal {} to commission by {} not found", pedestalId, installerUsername);
                    failed.add(pedestalId.value());
                } else {
                    pedestals.add(pedestal);
                }
            }
            if (!pedestals.isEmpty()) {
                for (Pedestal pedestal : affiliatePedestalService.commission(pedestals, installerUsername.value(),
                        token.value())) {
                    failed.add(pedestal.getId());
                }
            }
            if (!failed.isEmpty()) {
                LOG.info("Commissioning by {}: {} of {} pedestals not commissioned: {}",
                        installerUsername, failed.size(), pedestalIds.size(), failed);
            }
            return failed;
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public void commissionPedestal(Username installerUsername, PedestalCode code, Username ownerUsername,
                                   LocationDTO location, boolean listedInApp, String accessMode) {
        try {
            principalResolver.resolve(installerUsername, UserrightName.PEDESTAL_COMMISSION);
            principalResolver.findUser(ownerUsername);
            if (pedestalService.findByCode(code.value()) == null) {
                throw ObjectNotFoundException.of("Pedestal", code);
            }
            AccessMode mode = ApiValues.parseEnum(AccessMode.class, accessMode, "access mode");
            affiliatePedestalService.commission(code.value(), installerUsername.value(), ownerUsername.value(),
                    PedestalMapper.toLocation(location), listedInApp, mode);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public void decommissionPedestal(Username authUsername, PedestalId pedestalId) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.PEDESTAL_VIEW);
            Pedestal pedestal = pedestalService.findActive(pedestalId.value());
            if (pedestal == null) {
                throw ObjectNotFoundException.of("Pedestal", pedestalId);
            }
            if (!Boolean.TRUE.equals(affiliatePedestalService.isCommissioned(pedestal.getId()))) {
                throw new ConflictException("Pedestal is in the supplier stock already: " + pedestal.getCode());
            }
            affiliatePedestalService.decommission(pedestal.getId(), principal.getId());
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public List<String> importSupplierPedestals(Username authUsername, List<PedestalImportDTO> pedestals,
                                                boolean networkOwned) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.PEDESTAL_COMMISSION);
            if (pedestals == null || pedestals.isEmpty()) {
                throw new InvalidInputException("No pedestals to import");
            }
            if (pedestals.size() > MAX_PEDESTALS_PER_IMPORT_CALL) {
                throw new InvalidInputException(String.format("At most %d pedestals per call, got %d",
                        MAX_PEDESTALS_PER_IMPORT_CALL, pedestals.size()));
            }
            List<String> codes = new ArrayList<>();
            for (PedestalImportDTO dto : pedestals) {
                codes.add(supplierPedestalImportService.saveFromImport(PedestalMapper.toImportRow(dto), networkOwned)
                        .getCode());
            }
            auditService.auditAction(AuditActionType.PEDESTAL_IMPORT, principal.getUsername(),
                    codes.size() + " pedestals", null, String.join(",", codes), null);
            return codes;
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }
}
