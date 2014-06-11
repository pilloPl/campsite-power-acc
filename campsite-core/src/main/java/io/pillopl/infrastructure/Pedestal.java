package io.pillopl.infrastructure;

import io.pillopl.group.Group;
import io.pillopl.group.IsLinkedToGroup;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Hook-up pedestal on a campsite: 1-6 metered sockets and a modem, speaking one of the pedestal link generations.
 * <p>
 * Life cycle: delivered into the supplier stock ("SUPPLIER &lt;manufacturer&gt;" group), commissioned on a campsite
 * (SITE group of the campsite or its section), decommissioned back to the supplier. Soft delete only.
 * <p>
 * Flags: {@code billable} - consumption is billed through the network's ERP; {@code settledByCampsite} - the
 * affiliate campsite settles with its guests itself; {@code listedInApp} - shown in the guest app;
 * {@code automatedWhitelistUpdates} - card lists are pushed automatically; {@code managedExternally} - the pedestal
 * is connected through another operator's back office; {@code remoteControl} - remote start/stop allowed.
 */
@Entity
@Table(name = "PEDESTAL")
@Getter
@Setter
public class Pedestal implements IsLinkedToGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_pedestal")
    @SequenceGenerator(name = "seq_pedestal", sequenceName = "PEDESTAL_ID_SEQ", allocationSize = 1)
    private Long id;

    /**
     * Identity of the pedestal on the link (unique).
     */
    @Column(name = "CODE", length = 128, nullable = false, unique = true)
    private String code;

    @ManyToOne(optional = false)
    @JoinColumn(name = "PEDESTAL_TYPE_ID", nullable = false)
    private PedestalType pedestalType;

    @OneToMany(mappedBy = "pedestal", cascade = CascadeType.ALL)
    @OrderBy("position asc")
    @SQLRestriction("DELETED = false")
    private List<Socket> sockets = new ArrayList<>();

    @ManyToOne(optional = false)
    @JoinColumn(name = "LOCATION_ID", nullable = false)
    private Location location;

    @ManyToOne
    @JoinColumn(name = "GROUP_ID")
    private Group group;

    @Column(name = "REFERENCE_ID", length = 50)
    private String referenceId;

    @Column(name = "SERIAL_NUMBER", length = 64)
    private String serialNumber;

    @Column(name = "FIRMWARE_VERSION", length = 64)
    private String firmwareVersion;

    @Column(name = "MODEM_ICCID", length = 32)
    private String modemIccid;

    @Column(name = "YEAR_OF_INSTALLATION")
    private LocalDate yearOfInstallation;

    @Column(name = "DESCRIPTION", length = 128)
    private String description;

    @Column(name = "PRICE_INFORMATION", length = 50)
    private String priceInformation;

    /**
     * Address of the controller for messages of the pedestal backend; null when the pedestal can only send.
     */
    @Column(name = "LINK_URL", length = 255)
    private String linkUrl;

    @Column(name = "BILLABLE", nullable = false)
    private boolean billable;

    @Column(name = "SETTLED_BY_CAMPSITE", nullable = false)
    private boolean settledByCampsite;

    @Column(name = "LISTED_IN_APP", nullable = false)
    private boolean listedInApp = true;

    @Column(name = "AUTOMATED_WHITELIST_UPDATES", nullable = false)
    private boolean automatedWhitelistUpdates;

    @Column(name = "MANAGED_EXTERNALLY", nullable = false)
    private boolean managedExternally;

    @Column(name = "REMOTE_CONTROL", nullable = false)
    private boolean remoteControl = true;

    @Column(name = "DELETED", nullable = false)
    private boolean deleted;

    public Pedestal() {
    }

    public Pedestal(String code, PedestalType pedestalType, Location location) {
        this.code = code;
        this.pedestalType = pedestalType;
        this.location = location;
    }

    public void addSocket(Socket socket) {
        socket.setPedestal(this);
        sockets.add(socket);
    }

    public List<Long> getSocketIds() {
        List<Long> ids = new ArrayList<>(sockets.size());
        for (Socket socket : sockets) {
            ids.add(socket.getId());
        }
        return ids;
    }

    /**
     * @return the socket with the number used on the link, or null
     */
    public Socket findSocketBySocketNo(Integer socketNo) {
        if (socketNo == null) {
            return null;
        }
        for (Socket socket : sockets) {
            if (socketNo.equals(socket.getSocketNo())) {
                return socket;
            }
        }
        return null;
    }

    public Socket findSocketByPitchNo(String pitchNo) {
        for (Socket socket : sockets) {
            if (pitchNo != null && pitchNo.equals(socket.getPitchNo())) {
                return socket;
            }
        }
        return null;
    }

    public boolean hasSocketInError() {
        for (Socket socket : sockets) {
            if (socket.isMonitoring() && socket.getStatus().isError()) {
                return true;
            }
        }
        return false;
    }

    public String getManufacturer() {
        return pedestalType != null ? pedestalType.getManufacturer() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pedestal other)) return false;
        return Objects.equals(code, other.code);
    }

    @Override
    public int hashCode() {
        return 31 + Objects.hashCode(code);
    }

    @Override
    public String toString() {
        // e.g. "HR-BAY-P012 (Id: 1042)"
        return code + " (Id: " + id + ")";
    }
}
