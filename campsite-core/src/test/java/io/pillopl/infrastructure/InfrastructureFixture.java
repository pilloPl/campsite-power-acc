package io.pillopl.infrastructure;

import io.pillopl.group.Group;
import io.pillopl.persistence.dao.LocationDao;
import io.pillopl.persistence.dao.PedestalDao;
import io.pillopl.persistence.dao.PedestalTypeDao;
import io.pillopl.persistence.dao.SocketTypeDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Builds and saves pedestals with sockets for integration tests (seeded socket types, a test pedestal type).
 */
@Component
public class InfrastructureFixture {

    public static final String MANUFACTURER = "ACME PEDESTALS";

    @Autowired
    PedestalDao pedestalDao;

    @Autowired
    PedestalTypeDao pedestalTypeDao;

    @Autowired
    SocketTypeDao socketTypeDao;

    @Autowired
    LocationDao locationDao;

    public PedestalType pedestalType() {
        PedestalType type = pedestalTypeDao.findByManufacturerAndName(MANUFACTURER, "P4");
        if (type == null) {
            type = new PedestalType(MANUFACTURER, "P4", PedestalLinkVersion.V1);
            type.setLocalCardList(true);
            type.setLocalCardListCapacity(250);
            type.setMaxSockets(4);
            type = pedestalTypeDao.saveAndFlush(type);
        }
        return type;
    }

    public Location campsiteLocation(String name) {
        Location location = new Location("Rovinj", "52210", "Obala", "12");
        location.setName(name);
        location.setCountryIso("HR");
        return locationDao.saveAndFlush(location);
    }

    public Pedestal pedestal(String code, Group group, int sockets) {
        return pedestal(code, group, sockets, campsiteLocation("Bay campsite"));
    }

    public Pedestal pedestal(String code, Group group, int sockets, Location location) {
        Pedestal pedestal = new Pedestal(code, pedestalType(), location);
        pedestal.setGroup(group);
        SocketType ceeBlue = socketTypeDao.findByName("CEE blue 16A");
        for (int i = 1; i <= sockets; i++) {
            Socket socket = new Socket(code + "_" + i, ceeBlue, i);
            socket.setMonitoring(true);
            pedestal.addSocket(socket);
        }
        return pedestalDao.saveAndFlush(pedestal);
    }
}
