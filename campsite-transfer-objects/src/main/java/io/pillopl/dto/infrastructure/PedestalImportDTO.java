package io.pillopl.dto.infrastructure;

import java.io.Serializable;
import java.util.List;

/**
 * A pedestal of a supplier delivery (or of the console form): identification, type, address, group and sockets.
 */
public record PedestalImportDTO(Long id, String referenceId, String externalId, Boolean deleted, Boolean listedInApp,
                                String manufacturer, String typeName, String linkVersion, String serialNumber,
                                String modemIccid, Long groupId, String groupOwner, String accessibility,
                                String campsiteName, String campsiteCode, String street, String houseNumber,
                                String zipcode, String city, String countryIso,
                                List<ReceptionHoursDTO> receptionHours, List<SocketImportDTO> sockets)
        implements Serializable {

    public PedestalImportDTO {
        receptionHours = receptionHours != null ? List.copyOf(receptionHours) : List.of();
        sockets = sockets != null ? List.copyOf(sockets) : List.of();
    }
}
