package io.pillopl.infrastructure;

import io.pillopl.group.AccessMode;
import io.pillopl.user.Userinfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Mounting pedestals on an affiliate campsite. Staff open it for the affiliate owner (address, access mode,
 * listed in app); the installer refers to it by its token; it is closed (soft deleted) when the pedestals are
 * commissioned.
 */
@Entity
@Table(name = "COMMISSIONING")
@Getter
@Setter
public class Commissioning {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_commissioning")
    @SequenceGenerator(name = "seq_commissioning", sequenceName = "COMMISSIONING_ID_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "AFFILIATE_OWNER_ID", nullable = false)
    private Userinfo affiliateOwner;

    @ManyToOne(optional = false)
    @JoinColumn(name = "LOCATION_ID", nullable = false)
    private Location location;

    @Column(name = "COMMENTS", length = 1000)
    private String comments;

    @Column(name = "LISTED_IN_APP", nullable = false)
    private boolean listedInApp;

    @Enumerated(EnumType.STRING)
    @Column(name = "ACCESS_MODE", length = 32, nullable = false)
    private AccessMode accessMode = AccessMode.HOUSE_CARDS_ONLY;

    @Column(name = "TOKEN", length = 64, nullable = false, unique = true)
    private String token;

    @Column(name = "CREATE_DATE", nullable = false)
    private LocalDateTime createDate;

    @Column(name = "DELETE_DATE")
    private LocalDateTime deleteDate;

    @Column(name = "DELETED", nullable = false)
    private boolean deleted;

    public boolean isNetworkAccess() {
        return accessMode == AccessMode.NETWORK_ACCESS;
    }

    public void finish(LocalDateTime at) {
        this.deleted = true;
        this.deleteDate = at;
    }

    @Override
    public String toString() {
        return "Commissioning{id=" + id + ", owner=" + (affiliateOwner != null ? affiliateOwner.getUsername() : null)
                + ", " + accessMode + "}";
    }
}
