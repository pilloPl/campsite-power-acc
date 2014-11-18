package io.pillopl.power;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Section switchboard of a campsite. The sockets hanging on it (SOCKET.POWER_SECTION_ID) may together draw at most
 * {@link #maxCurrentA}; the bean named {@link #algorithmBeanName} decides whether one more supply fits.
 */
@Entity
@Table(name = "POWER_SECTION")
@Getter
@Setter
public class PowerSection {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_power_section")
    @SequenceGenerator(name = "seq_power_section", sequenceName = "POWER_SECTION_ID_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "SECTION_NAME", length = 128, nullable = false, unique = true)
    private String sectionName;

    @Column(name = "MAX_CURRENT_A", precision = 8, scale = 2, nullable = false)
    private BigDecimal maxCurrentA;

    @Column(name = "ALGORITHM_BEAN_NAME", length = 128)
    private String algorithmBeanName;

    public PowerSection() {
    }

    public PowerSection(String sectionName, BigDecimal maxCurrentA, String algorithmBeanName) {
        this.sectionName = sectionName;
        this.maxCurrentA = maxCurrentA;
        this.algorithmBeanName = algorithmBeanName;
    }

    public boolean hasAlgorithm() {
        return algorithmBeanName != null && !algorithmBeanName.isBlank();
    }

    @Override
    public String toString() {
        return "PowerSection{id=" + id + ", name='" + sectionName + "', maxCurrentA=" + maxCurrentA + ", algorithm='"
                + algorithmBeanName + "'}";
    }
}
