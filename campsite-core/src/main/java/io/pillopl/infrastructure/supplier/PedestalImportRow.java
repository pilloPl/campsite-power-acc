package io.pillopl.infrastructure.supplier;

import io.pillopl.infrastructure.PedestalLinkVersion;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * One pedestal of a supplier's delivery file (or of the console form): identification, type, address, group and
 * sockets.
 */
@Getter
@Setter
public class PedestalImportRow {

    /**
     * Id of an existing pedestal when the row comes from the console.
     */
    private Long id;

    private String referenceId;

    /**
     * The supplier's identification of the pedestal; becomes the code of pedestals not owned by the network.
     */
    private String externalId;

    private Boolean deleted;

    private Boolean listedInApp;

    private String manufacturer;

    private String typeName;

    private PedestalLinkVersion linkVersion = PedestalLinkVersion.V1;

    private String serialNumber;

    private String modemIccid;

    private Long groupId;

    private String groupOwner;

    private String accessibility;

    private String campsiteName;

    private String campsiteCode;

    private String street;

    private String houseNumber;

    private String zipcode;

    private String city;

    private String countryIso;

    private List<ReceptionHoursRow> receptionHours = new ArrayList<>();

    private List<SocketRow> sockets = new ArrayList<>();

    public record ReceptionHoursRow(DayOfWeek dayOfWeek, LocalTime from, LocalTime to) {
    }

    @Getter
    @Setter
    public static class SocketRow {

        private String externalId;

        private Integer pos;

        private String name;

        private String plugStandard;

        private Integer phases;

        private Integer amperes;

        private String pitchNo;
    }
}
