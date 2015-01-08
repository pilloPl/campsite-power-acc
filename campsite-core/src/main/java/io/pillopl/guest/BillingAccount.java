package io.pillopl.guest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Account in the ERP the consumption of a guest's cards is billed to. The cards carry its ERP number; an account
 * opened by an order has no number until the ERP assigns one.
 */
@Entity
@Table(name = "BILLING_ACCOUNT")
@Getter
@Setter
public class BillingAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_billing_account")
    @SequenceGenerator(name = "seq_billing_account", sequenceName = "BILLING_ACCOUNT_ID_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "GUEST_ID", nullable = false)
    private Guest guest;

    @Column(name = "ERP_ACCOUNT_NO", length = 40)
    private String erpAccountNo;

    @Column(name = "DESCRIPTION", length = 255)
    private String description;

    @Column(name = "CREATE_DATE", nullable = false)
    private LocalDateTime createDate = LocalDateTime.now();

    @Column(name = "CLOSED", nullable = false)
    private boolean closed;

    @Column(name = "CLOSE_DATE")
    private LocalDateTime closeDate;

    public BillingAccount() {
    }

    public BillingAccount(Guest guest, String erpAccountNo, String description) {
        this.guest = guest;
        this.erpAccountNo = erpAccountNo;
        this.description = description;
    }

    public boolean isAwaitingErpNumber() {
        return erpAccountNo == null || erpAccountNo.isBlank();
    }

    /**
     * Cards billed to the account are accepted: it is open and the ERP knows it.
     */
    public boolean isBillable() {
        return !closed && !isAwaitingErpNumber();
    }

    public boolean hasErpAccountNo(String accountNo) {
        return accountNo != null && accountNo.equals(erpAccountNo);
    }

    public void close(LocalDateTime at) {
        this.closed = true;
        this.closeDate = at;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BillingAccount other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "BillingAccount{id=" + id + ", erpAccountNo='" + erpAccountNo + "', closed=" + closed + "}";
    }
}
