package io.pillopl.issuer;

import io.pillopl.group.Group;
import io.pillopl.group.GroupType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A card issuer or an operator of externally managed campsites the network exchanges card lists and consumption
 * records with. The partner's cards hang in {@code issuerGroup}; its own campsites in {@code siteGroup}. The flags
 * switch the four directions of the exchange on per partner; {@code interfaceVersion} picks the transport.
 */
@Entity
@Table(name = "ISSUER_PARTNER")
@Getter
@Setter
public class IssuerPartner {

    static final int NAME_MAX_LENGTH = 100;

    static final int ENDPOINT_MAX_LENGTH = 500;

    /**
     * Interface generation of the partner: V1 CSV files in a directory synchronised by SFTP, V2 REST calls.
     */
    public enum InterfaceVersion {
        V1,
        V2
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_issuer_partner")
    @SequenceGenerator(name = "seq_issuer_partner", sequenceName = "ISSUER_PARTNER_ID_SEQ", allocationSize = 1)
    private Long id;

    /**
     * Short name, also the source name of the partner's imported consumption records.
     */
    @Column(name = "NAME", length = NAME_MAX_LENGTH, nullable = false, unique = true)
    private String name;

    @Column(name = "DISPLAY_NAME", length = 255)
    private String displayName;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ISSUER_GROUP_ID", nullable = false)
    private Group issuerGroup;

    @ManyToOne
    @JoinColumn(name = "SITE_GROUP_ID")
    private Group siteGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "INTERFACE_VERSION", length = 8, nullable = false)
    private InterfaceVersion interfaceVersion = InterfaceVersion.V1;

    /**
     * Directory of the files (V1) or base URL of the partner's REST interface (V2).
     */
    @Column(name = "ENDPOINT", length = ENDPOINT_MAX_LENGTH, nullable = false)
    private String endpoint;

    @Column(name = "CARD_IMPORT", nullable = false)
    private boolean cardImport;

    @Column(name = "CARD_EXPORT", nullable = false)
    private boolean cardExport;

    @Column(name = "RECORD_EXPORT", nullable = false)
    private boolean recordExport;

    @Column(name = "RECORD_IMPORT", nullable = false)
    private boolean recordImport;

    @Column(name = "ACTIVE", nullable = false)
    private boolean active = true;

    @Column(name = "DELETED", nullable = false)
    private boolean deleted;

    @Column(name = "CREATE_DATE", nullable = false)
    private LocalDateTime createDate = LocalDateTime.now();

    @OneToMany(mappedBy = "issuerPartner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IssuerSyncStatus> syncStatuses = new ArrayList<>();

    public IssuerPartner() {
    }

    public IssuerPartner(String name, Group issuerGroup, InterfaceVersion interfaceVersion, String endpoint) {
        this.name = name;
        this.issuerGroup = issuerGroup;
        this.interfaceVersion = interfaceVersion;
        this.endpoint = endpoint;
    }

    /**
     * The rules of ISSUER_PARTNER: a name of at most 100 characters, an endpoint, an ISSUER group of the cards, a SITE
     * group (the partner's own campsites) for the card export and the record import.
     *
     * @throws IllegalArgumentException naming the first broken rule
     */
    public void validate() {
        if (name == null || name.isBlank() || name.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("Partner name must have 1 to " + NAME_MAX_LENGTH + " characters");
        }
        if (endpoint == null || endpoint.isBlank() || endpoint.length() > ENDPOINT_MAX_LENGTH) {
            throw new IllegalArgumentException("Partner endpoint must have 1 to " + ENDPOINT_MAX_LENGTH
                    + " characters");
        }
        if (issuerGroup == null || !GroupType.ISSUER.name().equals(issuerGroup.getType())) {
            throw new IllegalArgumentException("The cards of a partner hang in an ISSUER group");
        }
        if (siteGroup != null && !GroupType.SITE.name().equals(siteGroup.getType())) {
            throw new IllegalArgumentException("The campsites of a partner hang in a SITE group");
        }
        if (siteGroup == null && (cardExport || recordImport)) {
            throw new IllegalArgumentException("Card export and record import need the SITE group of the partner");
        }
    }

    /**
     * The directions switched on for the partner, in the order of {@link IssuerSyncStatus.Direction}.
     */
    public List<IssuerSyncStatus.Direction> getDirections() {
        List<IssuerSyncStatus.Direction> directions = new ArrayList<>();
        for (IssuerSyncStatus.Direction direction : IssuerSyncStatus.Direction.values()) {
            boolean on = switch (direction) {
                case CARD_IMPORT -> cardImport;
                case CARD_EXPORT -> cardExport;
                case RECORD_EXPORT -> recordExport;
                case RECORD_IMPORT -> recordImport;
            };
            if (on) {
                directions.add(direction);
            }
        }
        return directions;
    }

    public boolean isExchanging() {
        return active && !deleted;
    }

    /**
     * The status row of the direction, created on first use.
     */
    public IssuerSyncStatus getSyncStatus(IssuerSyncStatus.Direction direction) {
        for (IssuerSyncStatus status : syncStatuses) {
            if (status.getDirection() == direction) {
                return status;
            }
        }
        IssuerSyncStatus status = new IssuerSyncStatus(this, direction);
        syncStatuses.add(status);
        return status;
    }

    @Override
    public String toString() {
        return "IssuerPartner{id=" + id + ", name='" + name + "', " + interfaceVersion + ", endpoint='" + endpoint
                + "'}";
    }
}
