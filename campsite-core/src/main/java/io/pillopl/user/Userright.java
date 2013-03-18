package io.pillopl.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * A single permission checked by the consoles, e.g. PEDESTAL_COMMISSION.
 */
@Entity
@Table(name = "USERRIGHT")
@Getter
@Setter
public class Userright {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_userrole")
    @SequenceGenerator(name = "seq_userrole", sequenceName = "USERROLE_ID_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "NAME", nullable = false, unique = true)
    private String name;

    public Userright() {
    }

    public Userright(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Userright other)) return false;
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
