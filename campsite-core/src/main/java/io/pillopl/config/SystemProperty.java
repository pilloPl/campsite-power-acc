package io.pillopl.config;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One configuration entry. The value is always kept as text; typed access lives in {@link SystemPropertyService}.
 */
@Entity
@Table(name = "SYSTEM_PROPERTY")
@Getter
@Setter
@NoArgsConstructor
public class SystemProperty {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_system_property")
    @SequenceGenerator(name = "seq_system_property", sequenceName = "SYSTEM_PROPERTY_ID_SEQ", allocationSize = 1)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "PROPERTY_KEY", nullable = false, unique = true, length = 100)
    private SystemPropertyKey key;

    @Column(name = "PROPERTY_VALUE", nullable = false, length = 4000)
    private String value;

    @Column(name = "DESCRIPTION", nullable = false, length = 1000)
    private String description;

    public SystemProperty(SystemPropertyKey key, String value, String description) {
        this.key = key;
        this.value = value;
        this.description = description;
    }

    @Override
    public String toString() {
        return String.format("%s(%s) %s=%s", getClass().getSimpleName(), id, key, value);
    }
}
