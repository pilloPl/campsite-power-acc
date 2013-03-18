package io.pillopl.user;

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

import java.util.ArrayList;
import java.util.List;

/**
 * A named bundle of roles (e.g. "RECEPTION DESK") given to users at once. Not a group of the organisation trees.
 */
@Entity
@Table(name = "SECURITYGROUP")
@Getter
@Setter
public class SecurityGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_userrole")
    @SequenceGenerator(name = "seq_userrole", sequenceName = "USERROLE_ID_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "NAME", unique = true, nullable = false)
    private String name;

    @ManyToMany
    @JoinTable(name = "SECURITYGROUP_USERROLE",
            joinColumns = @JoinColumn(name = "SECURITYGROUP_ID"),
            inverseJoinColumns = @JoinColumn(name = "USERROLE_ID"))
    private List<Userrole> roles = new ArrayList<>();

    @Override
    public String toString() {
        return "SecurityGroup{" + name + ", roles=" + roles + "}";
    }
}
