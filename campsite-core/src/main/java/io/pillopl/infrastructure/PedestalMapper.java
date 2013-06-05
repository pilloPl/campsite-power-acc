package io.pillopl.infrastructure;

import io.pillopl.api.exception.InvalidInputException;
import io.pillopl.dto.infrastructure.LocationDTO;
import io.pillopl.dto.infrastructure.PedestalDTO;
import io.pillopl.dto.infrastructure.PedestalImportDTO;
import io.pillopl.dto.infrastructure.PedestalSummaryDTO;
import io.pillopl.dto.infrastructure.PedestalTypeDTO;
import io.pillopl.dto.infrastructure.ReceptionHoursDTO;
import io.pillopl.dto.infrastructure.SocketDTO;
import io.pillopl.dto.infrastructure.SocketImportDTO;
import io.pillopl.dto.infrastructure.SupplierStockEntryDTO;
import io.pillopl.infrastructure.supplier.PedestalImportRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Pedestal, socket and location entities to transfer objects and back (location, supplier import rows).
 */
public final class PedestalMapper {

    private PedestalMapper() {
        throw new AssertionError();
    }

    /**
     * @param state link state of the pedestal, null when it never sent anything
     */
    public static PedestalDTO toDTO(Pedestal pedestal, PedestalState state) {
        if (pedestal == null) {
            return null;
        }
        List<SocketDTO> sockets = new ArrayList<>();
        for (Socket socket : pedestal.getSockets()) {
            if (!socket.isDeleted()) {
                sockets.add(toDTO(socket));
            }
        }
        return new PedestalDTO(pedestal.getId(), pedestal.getCode(), toDTO(pedestal.getPedestalType()),
                pedestal.getGroup() != null ? pedestal.getGroup().getId() : null,
                pedestal.getGroup() != null ? pedestal.getGroup().getDisplayName() : null,
                toDTO(pedestal.getLocation()), pedestal.getSerialNumber(), pedestal.getFirmwareVersion(),
                pedestal.getModemIccid(), pedestal.isBillable(), pedestal.isSettledByCampsite(),
                pedestal.isListedInApp(), pedestal.isAutomatedWhitelistUpdates(), pedestal.isManagedExternally(),
                pedestal.isRemoteControl(), pedestal.isDeleted(),
                state != null ? state.getApproxLastIncoming() : null,
                state == null || state.isTimeout(), sockets);
    }

    public static PedestalSummaryDTO toSummary(Pedestal pedestal) {
        int numberOfSockets = 0;
        for (Socket socket : pedestal.getSockets()) {
            if (!socket.isDeleted()) {
                numberOfSockets++;
            }
        }
        return new PedestalSummaryDTO(pedestal.getId(), pedestal.getCode(),
                pedestal.getPedestalType() != null ? pedestal.getPedestalType().getName() : null,
                pedestal.getGroup() != null ? pedestal.getGroup().getDisplayName() : null,
                pedestal.getLocation() != null ? pedestal.getLocation().getCity() : null, numberOfSockets,
                pedestal.hasSocketInError(), pedestal.isDeleted());
    }

    public static List<PedestalSummaryDTO> toSummaries(List<Pedestal> pedestals) {
        List<PedestalSummaryDTO> result = new ArrayList<>(pedestals.size());
        for (Pedestal pedestal : pedestals) {
            result.add(toSummary(pedestal));
        }
        return result;
    }

    public static SocketDTO toDTO(Socket socket) {
        if (socket == null) {
            return null;
        }
        return new SocketDTO(socket.getId(), socket.getName(), socket.getPosition(), socket.getSocketNo(),
                socket.getSocketCode(), socket.getPitchNo(),
                socket.getSocketType() != null ? socket.getSocketType().getName() : null, socket.getMeterKwh(),
                socket.getStatus() != null ? socket.getStatus().name() : null, socket.getLastStatusUpdate(),
                socket.isMonitoring(), socket.isFreeSocket());
    }

    public static PedestalTypeDTO toDTO(PedestalType type) {
        if (type == null) {
            return null;
        }
        return new PedestalTypeDTO(type.getId(), type.getManufacturer(), type.getName(),
                type.getLinkVersion() != null ? type.getLinkVersion().name() : null, type.isLocalCardList(),
                type.getLocalCardListCapacity(), type.getMaxSockets());
    }

    public static LocationDTO toDTO(Location location) {
        if (location == null) {
            return null;
        }
        List<ReceptionHoursDTO> hours = new ArrayList<>();
        for (ReceptionHours h : location.getReceptionHours()) {
            hours.add(new ReceptionHoursDTO(h.getDayOfWeek(), h.getOpenFrom(), h.getOpenTo()));
        }
        return new LocationDTO(location.getId(), location.getName(), location.getCampsiteCode(),
                location.getStreet(), location.getHouseNumber(), location.getZipcode(), location.getCity(),
                location.getRegion(), location.getCountryIso(), location.getLatitude(), location.getLongitude(),
                location.getNote(), hours);
    }

    /**
     * New (not persisted) location from the transfer object; the id of the transfer object is ignored.
     *
     * @throws InvalidInputException when the address is incomplete
     */
    public static Location toLocation(LocationDTO dto) {
        if (dto == null) {
            throw new InvalidInputException("Location is missing");
        }
        if (isBlank(dto.street()) || isBlank(dto.zipcode()) || isBlank(dto.city())) {
            throw new InvalidInputException("Street, zipcode and city of the location are needed");
        }
        Location location = new Location(dto.city(), dto.zipcode(), dto.street(), dto.houseNumber());
        location.setName(dto.name());
        location.setCampsiteCode(dto.campsiteCode() != null ? dto.campsiteCode().trim().toUpperCase(Locale.ROOT)
                : null);
        location.setRegion(dto.region());
        location.setCountryIso(dto.countryIso());
        location.setLatitude(dto.latitude());
        location.setLongitude(dto.longitude());
        location.setNote(dto.note());
        for (ReceptionHoursDTO h : dto.receptionHours()) {
            location.addReceptionHours(new ReceptionHours(h.dayOfWeek(), h.openFrom(), h.openTo()));
        }
        return location;
    }

    public static SupplierStockEntryDTO toDTO(SupplierStockEntry entry) {
        return new SupplierStockEntryDTO(entry.id(), entry.pedestalCode(), entry.pedestalType(),
                entry.numberOfSockets(), entry.modemIccid(), entry.serialNumber());
    }

    /**
     * @throws InvalidInputException for an unknown link version
     */
    public static PedestalImportRow toImportRow(PedestalImportDTO dto) {
        PedestalImportRow row = new PedestalImportRow();
        row.setId(dto.id());
        row.setReferenceId(dto.referenceId());
        row.setExternalId(dto.externalId());
        row.setDeleted(dto.deleted());
        row.setListedInApp(dto.listedInApp());
        row.setManufacturer(dto.manufacturer());
        row.setTypeName(dto.typeName());
        if (!isBlank(dto.linkVersion())) {
            try {
                row.setLinkVersion(PedestalLinkVersion.valueOf(dto.linkVersion().trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                throw new InvalidInputException("Unknown pedestal link version: " + dto.linkVersion(), e);
            }
        }
        row.setSerialNumber(dto.serialNumber());
        row.setModemIccid(dto.modemIccid());
        row.setGroupId(dto.groupId());
        row.setGroupOwner(dto.groupOwner());
        row.setAccessibility(dto.accessibility());
        row.setCampsiteName(dto.campsiteName());
        row.setCampsiteCode(dto.campsiteCode());
        row.setStreet(dto.street());
        row.setHouseNumber(dto.houseNumber());
        row.setZipcode(dto.zipcode());
        row.setCity(dto.city());
        row.setCountryIso(dto.countryIso());
        for (ReceptionHoursDTO h : dto.receptionHours()) {
            row.getReceptionHours().add(new PedestalImportRow.ReceptionHoursRow(h.dayOfWeek(), h.openFrom(),
                    h.openTo()));
        }
        for (SocketImportDTO s : dto.sockets()) {
            PedestalImportRow.SocketRow socketRow = new PedestalImportRow.SocketRow();
            socketRow.setExternalId(s.externalId());
            socketRow.setPos(s.position());
            socketRow.setName(s.name());
            socketRow.setPlugStandard(s.plugStandard());
            socketRow.setPhases(s.phases());
            socketRow.setAmperes(s.amperes());
            socketRow.setPitchNo(s.pitchNo());
            row.getSockets().add(socketRow);
        }
        return row;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
