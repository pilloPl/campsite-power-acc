package io.pillopl.infrastructure;

import io.pillopl.persistence.dao.LocationDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Campsite addresses.
 */
@Service
public class LocationService {

    private final LocationDao locationDao;

    public LocationService(LocationDao locationDao) {
        this.locationDao = locationDao;
    }

    /**
     * The location of pedestals in the supplier stock.
     */
    @Transactional
    public Location fetchOrCreateUnknownLocation() {
        Location unknown = locationDao.findFirstByNameAndDeletedFalseOrderById(Location.UNKNOWN);
        if (unknown == null) {
            unknown = new Location(Location.UNKNOWN, "00000", Location.UNKNOWN, "0");
            unknown.setName(Location.UNKNOWN);
            unknown = locationDao.saveAndFlush(unknown);
        }
        return unknown;
    }

    @Transactional
    public Location save(Location location) {
        return locationDao.save(location);
    }

    /**
     * Copy of an address (a commissioning gets its own copy, the address of the request stays untouched).
     */
    public static Location copyOf(Location source) {
        Location location = new Location(source.getCity(), source.getZipcode(), source.getStreet(),
                source.getHouseNumber());
        location.setName(source.getName());
        location.setCampsiteCode(source.getCampsiteCode());
        location.setRegion(source.getRegion());
        location.setCountryIso(source.getCountryIso() != null ? source.getCountryIso().toUpperCase() : null);
        location.setLatitude(source.getLatitude());
        location.setLongitude(source.getLongitude());
        location.setNote(source.getNote());
        return location;
    }
}
