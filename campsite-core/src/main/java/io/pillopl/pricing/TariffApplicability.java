package io.pillopl.pricing;

import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.Socket;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
 * A tariff assigned to one socket or to a whole pedestal from a start date on. New assignments are added, older
 * ones stay, so a supply is always priced with the assignment valid at its plug-in.
 */
@Entity
@Table(name = "TARIFF_APPLICABILITY")
@Getter
@Setter
public class TariffApplicability {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_tariff_applicability")
    @SequenceGenerator(name = "seq_tariff_applicability", sequenceName = "TARIFF_APPLICABILITY_ID_SEQ",
            allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "TARIFF_ID", nullable = false)
    private Tariff tariff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SOCKET_ID")
    private Socket socket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PEDESTAL_ID")
    private Pedestal pedestal;

    @Column(name = "START_DATE", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "CREATED_BY", length = 255)
    private String createdBy;

    @Column(name = "CREATE_DATE", nullable = false)
    private LocalDateTime createDate = LocalDateTime.now();

    public TariffApplicability() {
    }

    public static TariffApplicability forSocket(Socket socket, Tariff tariff, LocalDateTime startDate) {
        TariffApplicability applicability = new TariffApplicability();
        applicability.setSocket(socket);
        applicability.setTariff(tariff);
        applicability.setStartDate(startDate);
        return applicability;
    }

    public static TariffApplicability forPedestal(Pedestal pedestal, Tariff tariff, LocalDateTime startDate) {
        TariffApplicability applicability = new TariffApplicability();
        applicability.setPedestal(pedestal);
        applicability.setTariff(tariff);
        applicability.setStartDate(startDate);
        return applicability;
    }

    public boolean isForSocket() {
        return socket != null;
    }

    @Override
    public String toString() {
        return "TariffApplicability{id=" + id + ", socket=" + (socket != null ? socket.getId() : null)
                + ", pedestal=" + (pedestal != null ? pedestal.getId() : null) + ", startDate=" + startDate + "}";
    }
}
