package io.pillopl.card;

import io.pillopl.infrastructure.Pedestal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Version of the last local card list sent to the controller of a pedestal.
 */
@Entity
@Table(name = "LOCAL_LIST_VERSION")
@Getter
@Setter
public class LocalListVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_local_list_version")
    @SequenceGenerator(name = "seq_local_list_version", sequenceName = "LOCAL_LIST_VERSION_ID_SEQ",
            allocationSize = 1)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "PEDESTAL_ID", nullable = false, unique = true)
    private Pedestal pedestal;

    @Column(name = "VERSION", nullable = false)
    private Integer version;

    @Column(name = "MOD_DATE", nullable = false)
    private LocalDateTime modDate = LocalDateTime.now();

    public LocalListVersion() {
    }

    public LocalListVersion(Pedestal pedestal, Integer version) {
        this.pedestal = pedestal;
        this.version = version;
    }

    @Override
    public String toString() {
        return "LocalListVersion{pedestal=" + pedestal + ", version=" + version + "}";
    }
}
