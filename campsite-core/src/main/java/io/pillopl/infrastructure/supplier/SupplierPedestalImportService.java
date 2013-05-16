package io.pillopl.infrastructure.supplier;

import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.GroupServiceHelper;
import io.pillopl.group.GroupType;
import io.pillopl.group.Market;
import io.pillopl.group.MarketExtension;
import io.pillopl.group.attribute.SupplierMarkerService;
import io.pillopl.infrastructure.BreakerRating;
import io.pillopl.infrastructure.BreakerRatingSpecification;
import io.pillopl.infrastructure.Location;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.PedestalLinkVersion;
import io.pillopl.infrastructure.PedestalType;
import io.pillopl.infrastructure.PlugStandard;
import io.pillopl.infrastructure.ReceptionHours;
import io.pillopl.infrastructure.Socket;
import io.pillopl.infrastructure.SocketStatus;
import io.pillopl.infrastructure.SocketType;
import io.pillopl.persistence.dao.BreakerRatingSpecificationDao;
import io.pillopl.persistence.dao.GroupDao;
import io.pillopl.persistence.dao.LocationDao;
import io.pillopl.persistence.dao.PedestalDao;
import io.pillopl.persistence.dao.PedestalTypeDao;
import io.pillopl.persistence.dao.SocketDao;
import io.pillopl.persistence.dao.SocketTypeDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Import of pedestals from the supplier's delivery file (and saving from the console form): creates or updates the
 * pedestal with its type, address, reception hours, group and sockets.
 */
@Service
public class SupplierPedestalImportService {

    private static final Logger LOG = LoggerFactory.getLogger(SupplierPedestalImportService.class);

    private final PedestalDao pedestalDao;

    private final SocketDao socketDao;

    private final PedestalTypeDao pedestalTypeDao;

    private final SocketTypeDao socketTypeDao;

    private final BreakerRatingSpecificationDao breakerRatingSpecificationDao;

    private final LocationDao locationDao;

    private final GroupDao groupDao;

    private final GroupService groupService;

    private final SupplierMarkerService supplierMarkerService;

    public SupplierPedestalImportService(PedestalDao pedestalDao, SocketDao socketDao, PedestalTypeDao pedestalTypeDao,
                                         SocketTypeDao socketTypeDao,
                                         BreakerRatingSpecificationDao breakerRatingSpecificationDao,
                                         LocationDao locationDao, GroupDao groupDao, GroupService groupService,
                                         SupplierMarkerService supplierMarkerService) {
        this.pedestalDao = pedestalDao;
        this.socketDao = socketDao;
        this.pedestalTypeDao = pedestalTypeDao;
        this.socketTypeDao = socketTypeDao;
        this.breakerRatingSpecificationDao = breakerRatingSpecificationDao;
        this.locationDao = locationDao;
        this.groupDao = groupDao;
        this.groupService = groupService;
        this.supplierMarkerService = supplierMarkerService;
    }

    /**
     * @param row           the pedestal of the file
     * @param networkOwned  the network's own pedestal (code made from the address and the reference) or a pedestal
     *                      of a supplier delivery (code = the supplier's id)
     */
    @Transactional
    public Pedestal saveFromImport(PedestalImportRow row, boolean networkOwned) {
        boolean isNew = false;

        Pedestal pedestal = null;
        if (row.getId() != null) {
            // change an existing one from the console
            pedestal = pedestalDao.findById(row.getId()).orElse(null);
        } else if (row.getReferenceId() != null) {
            // change an existing one by import
            pedestal = pedestalDao.findByReferenceId(row.getReferenceId());
        } else if (row.getExternalId() != null) {
            // change an existing one by import
            pedestal = pedestalDao.findByCode(row.getExternalId());
        }
        if (pedestal == null) {
            isNew = true;
            // otherwise create it
            pedestal = new Pedestal();
            pedestal.setListedInApp(true);
        }

        if (isNew) {
            // create the final code of new pedestals
            String code;
            if (networkOwned) {
                String zipcode = "00000";
                if (row.getZipcode() != null && !row.getZipcode().isBlank()) {
                    zipcode = row.getZipcode().trim();
                }
                String country = row.getCountryIso() != null ? row.getCountryIso().trim() : "XX";
                String newCode = country + "-" + zipcode + "-" + row.getReferenceId();
                code = newCode.toUpperCase(Locale.ROOT).replaceAll(" ", "");
            } else {
                code = row.getExternalId();
            }
            pedestal.setCode(code);
        }

        // set group
        if (row.getGroupId() != null || row.getGroupOwner() != null) {
            Group group = findPedestalGroup(row.getGroupId(), row.getGroupOwner(),
                    SupplierAccessibility.parse(row.getAccessibility()));
            pedestal.setGroup(group);
        } else if (isNew) {
            // a new delivery goes into the stock of its manufacturer
            Group stock = groupDao.findByNotCaseSensitiveNameAndType(
                    GroupServiceHelper.getSupplierGroupName(row.getManufacturer()), GroupType.SITE.name());
            if (stock != null && supplierMarkerService.isInSupplierStock(stock)) {
                pedestal.setGroup(stock);
            } else {
                LOG.warn(String.format("No supplier stock for manufacturer '%s', pedestal %s has no group",
                        row.getManufacturer(), pedestal.getCode()));
            }
        }

        // set defaults
        if (row.getDeleted() != null) {
            pedestal.setDeleted(row.getDeleted());
        } else {
            pedestal.setDeleted(false);
        }
        if (row.getListedInApp() != null) {
            pedestal.setListedInApp(row.getListedInApp());
        } else {
            pedestal.setListedInApp(true);
        }
        pedestal.setBillable(false);

        PedestalType pedestalType = pedestalTypeDao.findByManufacturerAndName(row.getManufacturer(), row.getTypeName());
        if (pedestalType == null) {
            pedestalType = pedestalTypeDao.save(new PedestalType(row.getManufacturer(), row.getTypeName(),
                    row.getLinkVersion() != null ? row.getLinkVersion() : PedestalLinkVersion.V1));
        }
        pedestal.setPedestalType(pedestalType);

        if (networkOwned) {
            pedestal.setReferenceId(row.getReferenceId());
        } else {
            if (row.getReferenceId() != null) {
                pedestal.setReferenceId(row.getReferenceId());
            } else {
                pedestal.setReferenceId(row.getExternalId());
            }
        }
        if (row.getSerialNumber() != null) {
            pedestal.setSerialNumber(row.getSerialNumber());
        }
        if (row.getModemIccid() != null) {
            pedestal.setModemIccid(row.getModemIccid());
        }

        Location location = prepareLocation(row);
        prepareReceptionHours(row, location);
        pedestal.setLocation(location);

        if (pedestal.getId() == null) {
            pedestal = pedestalDao.save(pedestal);
        }

        // set delete flags and register as existing
        Map<String, Socket> existingSockets = new HashMap<>();
        if (pedestal.getSockets() != null && !pedestal.getSockets().isEmpty()) {
            for (Socket socket : pedestal.getSockets()) {
                socket.setDeleted(true);
                existingSockets.put(socket.getName(), socket);
            }
        }
        // update an existing socket (and remove the delete flag) or create a new one
        for (PedestalImportRow.SocketRow socketRow : row.getSockets()) {
            String externalId = socketRow.getExternalId();
            Integer pos = socketRow.getPos();

            String socketName;
            // the supplier's id of the socket wins
            if (externalId != null) {
                socketName = externalId;
            } else if (networkOwned) {
                socketName = pedestal.getCode() + "_" + pos;
            } else {
                socketName = socketRow.getName();
            }

            Socket socket = existingSockets.get(socketName);
            if (socket == null) {
                socket = new Socket();
                socket.setName(socketName);
            }

            socket.setDeleted(false);
            socket.setPedestal(pedestal);
            socket.setSocketNo(pos);
            socket.setMonitoring(false);
            socket.setMeterKwh(BigDecimal.ZERO);
            socket.setModDate(LocalDateTime.now());
            socket.setPosition(pos);
            socket.setStatus(SocketStatus.UNKNOWN);
            if (socketRow.getPitchNo() != null && !socketRow.getPitchNo().isBlank()) {
                socket.setPitchNo(socketRow.getPitchNo().trim().toUpperCase(Locale.ROOT));
            }

            socket.setSocketType(findOrCreateSocketType(socketRow));

            if (socket.getId() == null) {
                pedestal.addSocket(socket);
                socketDao.save(socket);
            }
        }

        // save at the end
        return pedestalDao.save(pedestal);
    }

    /**
     * The group of an imported pedestal: the given group (or the SITE group named after the owner) when the file
     * says nothing about the access, otherwise the access mode parent of its market.
     */
    Group findPedestalGroup(Long groupId, String ownerName, SupplierAccessibility accessibility) {
        Group group = groupId != null ? groupDao.findById(groupId).orElse(null)
                : groupDao.findByNotCaseSensitiveNameAndType(ownerName, GroupType.SITE.name());
        if (group == null) {
            throw new IllegalArgumentException(String.format("No SITE group %s / '%s' for the import", groupId,
                    ownerName));
        }
        Market market = groupService.determineMarketByGroupId(group.getId());
        if (market == null) {
            return group;
        }
        MarketExtension<Market> extension = groupService.getExtensionByMarket(market);
        return determineGroup(group, extension.findHouseCardsOnlySiteGroup(), extension.findNetworkAccessSiteGroup(),
                accessibility);
    }

    /**
     * Access mode from the file: public pedestals hang under NETWORK ACCESS, restricted ones under HOUSE CARDS ONLY,
     * anything else stays in the given group.
     */
    static Group determineGroup(Group group, Group houseCardsOnlyGroup, Group networkAccessGroup,
                                SupplierAccessibility accessibility) {
        switch (accessibility) {
            case PUBLIC_PAYING:
            case PUBLIC_FREE:
                return networkAccessGroup;
            case RESTRICTED:
                return houseCardsOnlyGroup;
            default:
                // fallback to the given group
                return group;
        }
    }

    private Location prepareLocation(PedestalImportRow row) {
        List<Location> existing = locationDao.findByAddress(row.getStreet(), row.getHouseNumber(), row.getZipcode(),
                row.getCity());
        Location location = existing.isEmpty()
                ? new Location(row.getCity(), row.getZipcode(), row.getStreet(), row.getHouseNumber())
                : existing.get(0);
        if (row.getCampsiteName() != null) {
            location.setName(row.getCampsiteName());
        }
        if (row.getCampsiteCode() != null) {
            location.setCampsiteCode(row.getCampsiteCode().toUpperCase(Locale.ROOT));
        }
        if (row.getCountryIso() != null) {
            location.setCountryIso(row.getCountryIso().toUpperCase(Locale.ROOT));
        }
        return locationDao.save(location);
    }

    /**
     * The reception hours of the file replace the ones of the location (when the file has any).
     */
    private void prepareReceptionHours(PedestalImportRow row, Location location) {
        if (row.getReceptionHours() == null || row.getReceptionHours().isEmpty()) {
            return;
        }
        location.getReceptionHours().clear();
        for (PedestalImportRow.ReceptionHoursRow hours : row.getReceptionHours()) {
            location.addReceptionHours(new ReceptionHours(hours.dayOfWeek(), hours.from(), hours.to()));
        }
    }

    private SocketType findOrCreateSocketType(PedestalImportRow.SocketRow socketRow) {
        PlugStandard plug = socketRow.getPlugStandard() != null
                ? PlugStandard.valueOf(socketRow.getPlugStandard().trim().toUpperCase(Locale.ROOT))
                : PlugStandard.CEE_BLUE_16A;
        int phases = socketRow.getPhases() != null ? socketRow.getPhases() : 1;
        BreakerRating rating = socketRow.getAmperes() != null ? BreakerRating.ofAmperes(socketRow.getAmperes()) : null;
        if (rating == null) {
            rating = BreakerRating.A16;
        }
        List<SocketType> types = socketTypeDao.findByTechnicalData(plug, phases, rating);
        if (!types.isEmpty()) {
            return types.get(0);
        }
        BreakerRatingSpecification specification = breakerRatingSpecificationDao.findByRating(rating);
        return socketTypeDao.save(new SocketType(plug.name() + " " + phases + "P " + rating, plug, phases,
                specification));
    }
}
