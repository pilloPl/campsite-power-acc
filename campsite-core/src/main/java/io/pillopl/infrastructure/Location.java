package io.pillopl.infrastructure;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Address of a campsite. Pedestals of the campsite point to it, a pedestal in the supplier stock points to the
 * {@link #UNKNOWN} location.
 */
@Entity
@Table(name = "LOCATION")
@Getter
@Setter
public class Location {

    public static final String UNKNOWN = "UNKNOWN";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_location")
    @SequenceGenerator(name = "seq_location", sequenceName = "LOCATION_ID_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "NAME", length = 256)
    private String name;

    /**
     * Short code of the campsite, prefix of the socket codes (see {@link SocketCodeHelper}).
     */
    @Column(name = "CAMPSITE_CODE", length = 16)
    private String campsiteCode;

    @Column(name = "STREET", length = 128, nullable = false)
    private String street;

    @Column(name = "HOUSE_NUMBER", length = 16, nullable = false)
    private String houseNumber;

    @Column(name = "ZIPCODE", length = 12, nullable = false)
    private String zipcode;

    @Column(name = "CITY", length = 128, nullable = false)
    private String city;

    @Column(name = "REGION", length = 128)
    private String region;

    @Column(name = "COUNTRY_ISO", length = 3)
    private String countryIso;

    @Column(name = "LATITUDE", precision = 14, scale = 10)
    private BigDecimal latitude;

    @Column(name = "LONGITUDE", precision = 14, scale = 10)
    private BigDecimal longitude;

    @Column(name = "NOTE", length = 256)
    private String note;

    @Column(name = "DELETED", nullable = false)
    private boolean deleted;

    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<ReceptionHours> receptionHours = new ArrayList<>();

    public Location() {
    }

    public Location(String city, String zipcode, String street, String houseNumber) {
        this.city = city;
        this.zipcode = zipcode;
        this.street = street;
        this.houseNumber = houseNumber;
    }

    public boolean isUnknown() {
        return UNKNOWN.equals(name);
    }

    public void addReceptionHours(ReceptionHours hours) {
        hours.setLocation(this);
        receptionHours.add(hours);
    }

    /**
     * One line address for mails, records and exports.
     */
    public String toAddressLine() {
        StringBuilder sb = new StringBuilder();
        sb.append(street).append(' ').append(houseNumber).append(", ").append(zipcode).append(' ').append(city);
        if (countryIso != null) {
            sb.append(", ").append(countryIso);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Location{id=" + id + ", name='" + name + "', " + toAddressLine() + "}";
    }
}
