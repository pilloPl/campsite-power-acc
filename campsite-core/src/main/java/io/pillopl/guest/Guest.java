package io.pillopl.guest;

import io.pillopl.group.Group;
import io.pillopl.group.IsLinkedToGroup;
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
import java.util.Objects;

/**
 * Guest of the network: holder of the billing account its camping cards are issued on. The guest hangs in the
 * ISSUER group of the card issuer; whoever manages that group manages the guest and its cards.
 */
@Entity
@Table(name = "GUEST")
@Getter
@Setter
public class Guest implements IsLinkedToGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_guest")
    @SequenceGenerator(name = "seq_guest", sequenceName = "GUEST_ID_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "GROUP_ID", nullable = false)
    private Group group;

    @Enumerated(EnumType.STRING)
    @Column(name = "GUEST_TYPE", length = 16, nullable = false)
    private GuestType guestType = GuestType.TRANSIT;

    @Column(name = "NAME", length = 255, nullable = false)
    private String name;

    @Column(name = "FIRST_NAME", length = 255)
    private String firstName;

    @Column(name = "EMAIL", length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 16, nullable = false)
    private GuestStatus status = GuestStatus.ACTIVE;

    @Column(name = "ERP_PARTNER_NO", length = 32)
    private String erpPartnerNo;

    @Column(name = "CREATE_DATE", nullable = false)
    private LocalDateTime createDate = LocalDateTime.now();

    @Column(name = "DELETED", nullable = false)
    private boolean deleted;

    public Guest() {
    }

    public Guest(Group group, GuestType guestType, String name) {
        this.group = group;
        this.guestType = guestType;
        this.name = name;
    }

    public boolean isDeactivated() {
        return status == GuestStatus.DEACTIVATED;
    }

    public boolean isSeasonal() {
        return guestType == GuestType.SEASONAL;
    }

    public String getFullName() {
        return firstName != null && !firstName.isBlank() ? firstName + " " + name : name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Guest other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Guest{id=" + id + ", name='" + getFullName() + "', " + guestType + ", " + status + "}";
    }
}
