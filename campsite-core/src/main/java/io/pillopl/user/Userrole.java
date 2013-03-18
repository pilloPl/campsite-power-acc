package io.pillopl.user;

import io.pillopl.group.GroupType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Role of a console user, a bundle of rights. {@code groupTypes} lists the trees (comma separated) in which a user
 * with this role may get groups; {@code selfService} marks the roles of the self-service console.
 * Roles are compared by name.
 */
@Entity
@Table(name = "USERROLE")
@Getter
@Setter
public class Userrole {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_userrole")
    @SequenceGenerator(name = "seq_userrole", sequenceName = "USERROLE_ID_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "NAME", nullable = false, unique = true)
    private String name;

    @Column(name = "GROUP_TYPES")
    private String groupTypes;

    @Column(name = "SORT_KEY", nullable = false)
    private Long sortKey;

    @Column(name = "SELF_SERVICE", nullable = false)
    private boolean selfService;

    @ManyToMany
    @JoinTable(name = "USERROLE_USERRIGHT",
            joinColumns = @JoinColumn(name = "ROLE_ID"),
            inverseJoinColumns = @JoinColumn(name = "RIGHT_ID"))
    private Set<Userright> rights = new HashSet<>();

    public Userrole() {
    }

    public Userrole(String name) {
        this.name = name;
    }

    public Userrole(UserroleName name) {
        this(name.name());
    }

    public boolean is(UserroleName roleName) {
        return roleName.name().equals(name);
    }

    public boolean hasRight(String rightName) {
        return rights.contains(new Userright(rightName));
    }

    /**
     * @return true when users with this role may hang in groups of the type (no restriction when not configured)
     */
    public boolean allowsGroupType(GroupType groupType) {
        if (groupTypes == null || groupTypes.isBlank()) {
            return true;
        }
        for (String type : groupTypes.split(",")) {
            if (type.trim().equals(groupType.name())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Userrole other)) return false;
        return Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return 31 + Objects.hashCode(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
