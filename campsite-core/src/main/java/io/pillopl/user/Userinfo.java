package io.pillopl.user;

import io.pillopl.group.Group;
import io.pillopl.group.GroupType;
import io.pillopl.group.IsLinkedToGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Console user (staff, affiliate owner, seasonal resident).
 * <p>
 * {@link #getGroup()} is the user's own place in the STAFF tree: whoever manages that group manages the user.
 * {@link #getGroups()} holds the scope of the user, at most one group per tree ({@link #getGroup(GroupType)}):
 * the STAFF group whose users it administers, the SITE group whose pedestals it sees, the ISSUER group whose guests
 * and cards it sees.
 */
@Entity
@Table(name = "USERINFO")
@Getter
@Setter
public class Userinfo implements IsLinkedToGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_userinfo")
    @SequenceGenerator(name = "seq_userinfo", sequenceName = "USERINFO_ID_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "USERNAME", length = 128, nullable = false, unique = true)
    private String username;

    @Column(name = "PASSWORD_HASH", length = 256)
    private String passwordHash;

    @Column(name = "NAME", length = 256, nullable = false)
    private String name;

    @Column(name = "EMAIL", length = 256, nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 16, nullable = false)
    private UserinfoStatus status = UserinfoStatus.INVITED;

    @Column(name = "EMAIL_LOCALE", length = 8)
    private String emailLocale;

    @ManyToOne
    @JoinColumn(name = "GROUP_ID")
    private Group group;

    @ManyToMany
    @JoinTable(name = "USERINFO_GROUP",
            joinColumns = @JoinColumn(name = "USERINFO_ID"),
            inverseJoinColumns = @JoinColumn(name = "GROUP_ID"))
    private List<Group> groups = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "USERINFO_USERROLE",
            joinColumns = @JoinColumn(name = "USERINFO_ID"),
            inverseJoinColumns = @JoinColumn(name = "USERROLE_ID"))
    private List<Userrole> roles = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "USERINFO_SECURITYGROUP",
            joinColumns = @JoinColumn(name = "USERINFO_ID"),
            inverseJoinColumns = @JoinColumn(name = "SECURITYGROUP_ID"))
    private Set<SecurityGroup> securityGroups = new HashSet<>();

    /**
     * ERP partner number of an affiliate owner whose campsite settles with the network (see
     * {@code UserinfoService.findSettlementPartnerNo}).
     */
    @Column(name = "SETTLEMENT_PARTNER_NO")
    private String settlementPartnerNo;

    @Column(name = "CREATE_DATE", nullable = false)
    private LocalDateTime createDate = LocalDateTime.now();

    @Column(name = "DELETED", nullable = false)
    private boolean deleted;

    public Userinfo() {
    }

    public Userinfo(String username, String name, String email) {
        this.username = username;
        this.name = name;
        this.email = email;
    }

    /**
     * @return the group of the given tree from the scope groups, or null
     */
    public Group getGroup(GroupType type) {
        return getGroup(type.name());
    }

    public Group getGroup(String type) {
        for (Group g : groups) {
            if (g.getType().equals(type)) {
                return g;
            }
        }
        return null;
    }

    public boolean containsRole(String roleName) {
        return roles != null && roles.contains(new Userrole(roleName)); // Userrole equality is by name
    }

    public boolean containsRole(UserroleName roleName) {
        return containsRole(roleName.name());
    }

    public boolean removeRole(String roleName) {
        return roles != null && roles.remove(new Userrole(roleName));
    }

    public boolean hasSelfServiceRole() {
        for (Userrole role : roles) {
            if (role.isSelfService()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Direct roles and the roles of the security groups.
     */
    public Set<Userrole> getEffectiveRoles() {
        Set<Userrole> result = new LinkedHashSet<>(roles);
        for (SecurityGroup securityGroup : securityGroups) {
            result.addAll(securityGroup.getRoles());
        }
        return result;
    }

    public boolean hasRight(String rightName) {
        for (Userrole role : getEffectiveRoles()) {
            if (role.hasRight(rightName)) {
                return true;
            }
        }
        return false;
    }

    public Locale getLocale() {
        return emailLocale != null ? Locale.forLanguageTag(emailLocale) : Locale.ENGLISH;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Userinfo other)) return false;
        return Objects.equals(username, other.username);
    }

    @Override
    public int hashCode() {
        return 31 + Objects.hashCode(username);
    }

    @Override
    public String toString() {
        return "Userinfo{id=" + id + ", username='" + username + "'}";
    }
}
