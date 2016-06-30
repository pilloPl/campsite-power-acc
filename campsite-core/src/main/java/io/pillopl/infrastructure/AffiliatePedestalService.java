package io.pillopl.infrastructure;

import io.pillopl.audit.AuditActionType;
import io.pillopl.audit.AuditService;
import io.pillopl.config.SystemPropertyService;
import io.pillopl.event.EventService;
import io.pillopl.event.PedestalCommissionedEvent;
import io.pillopl.event.PedestalDecommissionedEvent;
import io.pillopl.group.AccessDeniedException;
import io.pillopl.group.AccessMode;
import io.pillopl.group.AffiliateGroupService;
import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.GroupServiceHelper;
import io.pillopl.group.GroupType;
import io.pillopl.group.Market;
import io.pillopl.group.attribute.SupplierMarker;
import io.pillopl.group.attribute.SupplierMarkerService;
import io.pillopl.persistence.dao.GroupDao;
import io.pillopl.persistence.dao.LocationDao;
import io.pillopl.monitoring.PedestalCommunicationLog;
import io.pillopl.monitoring.PedestalCommunicationLog.LogType;
import io.pillopl.persistence.dao.PedestalCommunicationLogDao;
import io.pillopl.persistence.dao.PedestalDao;
import io.pillopl.persistence.dao.SupplierMarkerDao;
import io.pillopl.persistence.dao.UserinfoDao;
import io.pillopl.user.AffiliateSetupException;
import io.pillopl.user.Userinfo;
import io.pillopl.user.UserinfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Pedestals of affiliate campsites: the supplier stock, commissioning on the campsite (group, address, socket codes,
 * access mode) and decommissioning back to the supplier.
 */
@Service
public class AffiliatePedestalService {

    private static final Logger LOG = LoggerFactory.getLogger(AffiliatePedestalService.class);

    private final GroupDao groupDao;

    private final UserinfoDao userinfoDao;

    private final LocationDao locationDao;

    private final PedestalDao pedestalDao;

    private final SupplierMarkerDao supplierMarkerDao;

    private final GroupService groupService;

    private final AffiliateGroupService affiliateGroupService;

    private final SupplierMarkerService supplierMarkerService;

    private final CommissioningService commissioningService;

    private final PedestalService pedestalService;

    private final UserinfoService userinfoService;

    private final LocationService locationService;

    private final SystemPropertyService systemPropertyService;

    private final DecommissioningNotifier decommissioningNotifier;

    private final AuditService auditService;

    private final EventService eventService;

    private final PedestalCommunicationLogDao pedestalCommunicationLogDao;

    public AffiliatePedestalService(GroupDao groupDao, UserinfoDao userinfoDao, LocationDao locationDao,
                                    PedestalDao pedestalDao, SupplierMarkerDao supplierMarkerDao,
                                    GroupService groupService, AffiliateGroupService affiliateGroupService,
                                    SupplierMarkerService supplierMarkerService,
                                    CommissioningService commissioningService, PedestalService pedestalService,
                                    UserinfoService userinfoService, LocationService locationService,
                                    SystemPropertyService systemPropertyService,
                                    DecommissioningNotifier decommissioningNotifier, AuditService auditService,
                                    EventService eventService,
                                    PedestalCommunicationLogDao pedestalCommunicationLogDao) {
        this.groupDao = groupDao;
        this.userinfoDao = userinfoDao;
        this.locationDao = locationDao;
        this.pedestalDao = pedestalDao;
        this.supplierMarkerDao = supplierMarkerDao;
        this.groupService = groupService;
        this.affiliateGroupService = affiliateGroupService;
        this.supplierMarkerService = supplierMarkerService;
        this.commissioningService = commissioningService;
        this.pedestalService = pedestalService;
        this.userinfoService = userinfoService;
        this.locationService = locationService;
        this.systemPropertyService = systemPropertyService;
        this.decommissioningNotifier = decommissioningNotifier;
        this.auditService = auditService;
        this.eventService = eventService;
        this.pedestalCommunicationLogDao = pedestalCommunicationLogDao;
    }

    /**
     * Pedestals in the supplier stock groups the user sees (its SITE group and the groups accepted for it).
     */
    @Transactional(readOnly = true)
    public List<SupplierStockEntry> supplierStock(String username) {
        Userinfo user = userinfoDao.findByUsername(username);

        Group group = user.getGroup(GroupType.SITE);
        Set<Group> allGroups = new LinkedHashSet<>();
        if (group != null) {
            allGroups.add(group);
            allGroups.addAll(GroupServiceHelper.collectSubtree(group));
        }

        List<Long> groupIds = new ArrayList<>();
        for (Group g : allGroups) {
            if (supplierMarkerService.isInSupplierStock(g)) groupIds.add(g.getId());
        }

        List<SupplierStockEntry> list = new ArrayList<>();
        if (!groupIds.isEmpty()) {
            for (Pedestal pedestal : pedestalDao.findByGroupIds(groupIds)) {
                LocalDateTime lastBoot = null;
                List<PedestalCommunicationLog> logs = pedestalCommunicationLogDao.getLogs(pedestal.getId(),
                        LogType.BOOT);
                if (logs != null && !logs.isEmpty()) {
                    lastBoot = logs.get(0).getDate();
                }

                LocalDateTime lastConsumptionRecord = null;
                logs = pedestalCommunicationLogDao.getLogs(pedestal.getId(), LogType.CONSUMPTION_RECORD);
                if (logs != null && !logs.isEmpty()) {
                    lastConsumptionRecord = logs.get(0).getDate();
                }
                list.add(new SupplierStockEntry(pedestal.getId(), pedestal.getCode(),
                        pedestal.getPedestalType().getName(), pedestal.getSockets().size(), pedestal.getModemIccid(),
                        pedestal.getSerialNumber(), lastBoot, lastConsumptionRecord));
            }
        }
        return list;
    }

    /**
     * Commissions the pedestals of an open commissioning on the affiliate campsite of its owner, then sets the
     * access mode and closes the commissioning. Does not check the installer's access to the pedestals, only to
     * the owner.
     *
     * @return the pedestals that could not be commissioned
     */
    @Transactional
    public List<Pedestal> commission(List<Pedestal> pedestals, String installerUsername, String token) {
        List<Pedestal> failed = new ArrayList<>();

        Commissioning commissioning = commissioningService.findByToken(installerUsername, token);
        if (commissioning == null) {
            LOG.error("No commissioning found for user: " + installerUsername + " and token: " + token);
            return pedestals;
        }

        Group affiliateGroup = affiliateGroupService.findAffiliateSiteGroup(
                commissioning.getAffiliateOwner().getUsername());
        if (affiliateGroup == null) {
            String message = "Seems something is wrong with the affiliate owner's group setup. There is no affiliate "
                    + "group (commissioning with id: " + commissioning.getId() + ")";
            LOG.error(message);
            throw new AffiliateSetupException(message);
        }

        for (Pedestal pedestal : pedestals) {
            try {
                Pedestal p = pedestalDao.findById(pedestal.getId()).orElseThrow();
                if (!isCommissioned(pedestal.getId())) {
                    commission(p, affiliateGroup, LocationService.copyOf(commissioning.getLocation()));
                    auditService.auditAction(AuditActionType.PEDESTAL_COMMISSIONING, installerUsername, p.getCode(),
                            String.valueOf(affiliateGroup.getId()), "commissioning " + commissioning.getId(), null);
                    eventService.send(new PedestalCommissionedEvent(p.getId(), p.getCode(), affiliateGroup.getId(),
                            installerUsername));
                }
            } catch (RuntimeException e) {
                failed.add(pedestal);
                LOG.error("Error commissioning pedestal " + pedestal, e);
                auditService.auditAction(AuditActionType.PEDESTAL_COMMISSIONING, installerUsername,
                        String.valueOf(pedestal.getId()), String.valueOf(affiliateGroup.getId()),
                        "commissioning " + commissioning.getId(), e.getMessage());
            }
        }
        switchAccessMode(affiliateGroup.getId(), commissioning);
        commissioningService.finish(commissioning);

        return failed;
    }

    /**
     * Commissions one pedestal directly (without a prepared commissioning) for an affiliate owner.
     */
    @Transactional
    public void commission(String pedestalCode, String installerUsername, String ownerUsername, Location location,
                           boolean listedInApp, AccessMode accessMode) {
        Userinfo installer = userinfoDao.findByUsername(installerUsername);
        Userinfo owner = userinfoService.checkAffiliateOwnerSetup(ownerUsername);
        if (owner == null) {
            throw new AffiliateSetupException("Affiliate group of the owner seems not configured yet");
        }
        Group affiliateGroup = affiliateGroupService.findAffiliateSiteGroup(ownerUsername);
        boolean installerHasRights = installer != null && commissioningService.checkInstallerRights(installer, owner);
        if (!installerHasRights) {
            throw new AccessDeniedException("Installer (" + installerUsername + ") has no rights to administer user "
                    + owner);
        }

        Pedestal pedestal = pedestalDao.findByCode(pedestalCode);
        if (pedestal == null) {
            throw new IllegalArgumentException("Could not find pedestal with code: " + pedestalCode);
        }

        if (isCommissioned(pedestal.getId())) {
            throw new IllegalStateException("Pedestal already commissioned");
        }

        Commissioning commissioning = new Commissioning();
        commissioning.setAffiliateOwner(owner);
        commissioning.setLocation(location);
        commissioning.setListedInApp(listedInApp);
        commissioning.setAccessMode(accessMode);
        commissioning = commissioningService.create(commissioning);

        commission(pedestal, affiliateGroup, LocationService.copyOf(location));
        auditService.auditAction(AuditActionType.PEDESTAL_COMMISSIONING, installerUsername, pedestal.getCode(),
                String.valueOf(affiliateGroup.getId()), "direct commissioning for " + ownerUsername, null);
        eventService.send(new PedestalCommissionedEvent(pedestal.getId(), pedestal.getCode(), affiliateGroup.getId(),
                installerUsername));
        switchAccessMode(affiliateGroup.getId(), commissioning);
        commissioningService.finish(commissioning);
    }

    /**
     * A pedestal is commissioned when its group is no supplier stock.
     *
     * @return null when the pedestal does not exist
     */
    @Transactional(readOnly = true)
    public Boolean isCommissioned(Long pedestalId) {
        Boolean isCommissioned = null;
        Pedestal pedestal = pedestalService.find(pedestalId);
        if (pedestal != null) {
            isCommissioned = pedestal.getGroup() == null || !supplierMarkerService.isInSupplierStock(pedestal.getGroup());
        }
        return isCommissioned;
    }

    /**
     * All pedestals in an active supplier stock.
     */
    @Transactional(readOnly = true)
    public List<Pedestal> findNotCommissioned() {
        List<Pedestal> pedestals = new ArrayList<>();
        List<SupplierMarker> markers = supplierMarkerDao.findAll();
        if (!markers.isEmpty()) {
            List<Long> groupIds = new ArrayList<>();
            for (SupplierMarker marker : markers) {
                if (marker.isActive()) {
                    groupIds.add(marker.getGroup().getId());
                }
            }
            Set<Long> ids = groupService.collectSubtreeIdsOfAll(groupIds.toArray(new Long[0]));
            pedestals = pedestalService.getByGroupIds(new ArrayList<>(ids));
        }
        return pedestals;
    }

    /**
     * The affiliate owner sends a pedestal back to the supplier: supplier stock group, UNKNOWN location, defaults,
     * socket codes removed; the owner is told unless his market is excluded from the mail.
     */
    @Transactional
    public void decommission(Long pedestalId, Long userId) {
        Pedestal pedestal = pedestalDao.findById(pedestalId).orElseThrow();
        Userinfo userinfo = userinfoDao.findById(userId).orElseThrow();
        pedestalService.checkIsManageable(userinfo, pedestal);
        defaultPedestalForDecommission(pedestal);
        pedestalDao.save(pedestal);
        auditService.auditAction(AuditActionType.PEDESTAL_DECOMMISSIONING, userinfo.getUsername(), pedestal.getCode(),
                String.valueOf(pedestal.getGroup().getId()), "back to supplier", null);
        eventService.send(new PedestalDecommissionedEvent(pedestal.getId(), pedestal.getCode(),
                pedestal.getGroup().getId(), userinfo.getUsername()));

        Collection<Market> excludeMarkets = excludedMarkets();

        Group affiliateGroup = affiliateGroupService.findAffiliateSiteGroup(userinfo.getUsername());
        Market userMarket = null;
        if (affiliateGroup != null) {
            userMarket = groupService.determineMarketByGroupId(affiliateGroup.getId());
        }
        if (affiliateGroup == null || (userMarket != null && !excludeMarkets.contains(userMarket))) {
            decommissioningNotifier.pedestalBackToSupplier(userinfo.getLocale(), pedestal.getCode(),
                    userinfo.getEmail());
        }
    }

    void defaultPedestalForDecommission(Pedestal pedestal) {
        Group supplierGroup = findSupplierGroupForPedestal(pedestal);
        if (supplierGroup == null) {
            throw new IllegalStateException("No supplier stock for manufacturer " + pedestal.getManufacturer());
        }
        pedestal.setGroup(supplierGroup);
        pedestal.setLocation(locationService.placeholderLocation());
        defaultPedestal(pedestal);
    }

    Group findSupplierGroupForPedestal(Pedestal pedestal) {
        String name = GroupServiceHelper.getSupplierGroupName(pedestal.getManufacturer());
        return groupDao.findByNameIgnoringCaseAndType(name, GroupType.SITE.name());
    }

    private Collection<Market> excludedMarkets() {
        Set<Market> markets = new HashSet<>();
        String excluded = systemPropertyService.getDecommissioningMailExcludedMarkets();
        if (excluded != null && !excluded.isBlank()) {
            for (String name : excluded.split(",")) {
                try {
                    markets.add(Market.valueOf(name.trim()));
                } catch (IllegalArgumentException e) {
                    LOG.warn("Unknown market in DecommissioningMailExcludedMarkets: " + name);
                }
            }
        }
        return markets;
    }

    private void defaultPedestal(Pedestal pedestal) {
        pedestal.setSettledByCampsite(false);
        pedestal.setRemoteControl(true);
        pedestal.setPriceInformation("");
        pedestal.setDescription("");
        pedestal.setBillable(false);
        SocketCodeHelper.setSocketCodesToNull(pedestal.getSockets());
        for (Socket socket : pedestal.getSockets()) {
            socket.setPitchNo(null);
            socket.setFreeSocket(false);
        }
    }

    private void switchAccessMode(Long affiliateGroupId, Commissioning commissioning) {
        LOG.info("Set the affiliate group's access mode for commissioning with id: " + commissioning.getId());
        affiliateGroupService.switchAccessMode(affiliateGroupId, commissioning.getAccessMode(),
                commissioning.isListedInApp());
    }

    private void commission(Pedestal pedestal, Group affiliateGroup, Location location) {
        Objects.requireNonNull(pedestal);
        Objects.requireNonNull(affiliateGroup);
        Objects.requireNonNull(location);

        locationDao.save(location);
        pedestal.setGroup(affiliateGroup);
        pedestal.setLocation(location);
        pedestal.setBillable(true);
        SocketCodeHelper.setSocketCodes(pedestal, location);
        pedestalDao.save(pedestal);
    }
}
