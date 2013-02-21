package io.pillopl.infrastructure;

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

import java.util.Objects;

/**
 * Type of a socket: plug standard, phases and the breaker that protects it.
 */
@Entity
@Table(name = "SOCKET_TYPE")
@Getter
@Setter
public class SocketType {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_socket_type")
    @SequenceGenerator(name = "seq_socket_type", sequenceName = "SOCKET_TYPE_ID_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "NAME", length = 128, nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "PLUG_STANDARD", length = 32, nullable = false)
    private PlugStandard plugStandard;

    @Column(name = "PHASES", nullable = false)
    private int phases = 1;

    @ManyToOne(optional = false)
    @JoinColumn(name = "BREAKER_RATING_ID", nullable = false)
    private BreakerRatingSpecification breakerRating;

    @Column(name = "DELETED", nullable = false)
    private boolean deleted;

    public SocketType() {
    }

    public SocketType(String name, PlugStandard plugStandard, int phases, BreakerRatingSpecification breakerRating) {
        this.name = name;
        this.plugStandard = plugStandard;
        this.phases = phases;
        this.breakerRating = breakerRating;
    }

    /**
     * Same technical data (used by the supplier import to reuse an existing type instead of creating a copy).
     */
    public boolean sameAs(PlugStandard otherPlug, int otherPhases, BreakerRating otherRating) {
        return plugStandard == otherPlug && phases == otherPhases && breakerRating != null
                && breakerRating.getRating() == otherRating;
    }

    public BreakerRating getRating() {
        return breakerRating != null ? breakerRating.getRating() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SocketType other)) return false;
        return Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public String toString() {
        return "SocketType{id=" + id + ", name='" + name + "', " + plugStandard + ", " + getRating() + "}";
    }
}
