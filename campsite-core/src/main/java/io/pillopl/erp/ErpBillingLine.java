package io.pillopl.erp;

import io.pillopl.consumption.ExportStatus;
import io.pillopl.guest.BillingAccount;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * One line of a bill handed to the ERP: a billing account, an ERP article and its quantity and amount in a billing
 * period, with the ERP constants of the run. Lines of an account below the minimum bill are never created.
 */
@Entity
@Table(name = "ERP_BILLING_LINE")
@Getter
@Setter
public class ErpBillingLine {

    public enum LineType {
        /**
         * share of the contract's fixed fee in the period
         */
        FIXED_FEE,
        /**
         * kWh above the contract's package, priced by the contract's tariff
         */
        PACKAGE_OVERAGE,
        /**
         * power of supplies outside a contract, at the cost of their consumption records
         */
        POWER
    }

    public static final int UNIT_PRICE_SCALE = 4;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_erp_billing_line")
    @SequenceGenerator(name = "seq_erp_billing_line", sequenceName = "ERP_BILLING_LINE_ID_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "BILLING_PERIOD", length = 7, nullable = false)
    private String billingPeriod;

    @ManyToOne(optional = false)
    @JoinColumn(name = "BILLING_ACCOUNT_ID", nullable = false)
    private BillingAccount billingAccount;

    @Column(name = "BILLING_ACCOUNT_NO", length = 40, nullable = false)
    private String billingAccountNo;

    @Column(name = "ERP_PARTNER_NO", length = 32)
    private String erpPartnerNo;

    @Column(name = "ORDER_NUMBER", length = 32)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "LINE_TYPE", length = 16, nullable = false)
    private LineType lineType;

    @Column(name = "ARTICLE_NO", length = 32, nullable = false)
    private String articleNo;

    @Column(name = "DESCRIPTION", length = 255)
    private String description;

    @Column(name = "QUANTITY", precision = 19, scale = 4, nullable = false)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "UNIT_PRICE", precision = 19, scale = 4, nullable = false)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "AMOUNT", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "CURRENCY", length = 3, nullable = false)
    private String currency = "EUR";

    @Column(name = "TAX_CLASS", length = 16)
    private String taxClass;

    @Column(name = "ARTICLE_CLASSIFICATION", length = 16)
    private String articleClassification;

    @Column(name = "SALES_OFFICE", length = 16)
    private String salesOffice;

    @Column(name = "SALES_GROUP", length = 16)
    private String salesGroup;

    @Column(name = "ASSIGNEE", length = 32)
    private String assignee;

    @Column(name = "ORDER_TYPE", length = 16)
    private String orderType;

    @Column(name = "ALTERNATE_TEXT", length = 255)
    private String alternateText;

    @Enumerated(EnumType.STRING)
    @Column(name = "EXPORT_STATUS", length = 16, nullable = false)
    private ExportStatus exportStatus = ExportStatus.NEW;

    @Column(name = "ERROR_TEXT", length = 1000)
    private String errorText;

    @Column(name = "CREATE_DATE", nullable = false)
    private LocalDateTime createDate;

    @Column(name = "EXPORT_DATE")
    private LocalDateTime exportDate;

    @Column(name = "ANSWER_DATE")
    private LocalDateTime answerDate;

    public ErpBillingLine() {
    }

    public ErpBillingLine(String billingPeriod, BillingAccount billingAccount, LineType lineType, String articleNo) {
        this.billingPeriod = billingPeriod;
        this.billingAccount = billingAccount;
        this.billingAccountNo = billingAccount.getErpAccountNo();
        this.lineType = lineType;
        this.articleNo = articleNo;
    }

    /**
     * Adds a part to the line (two contracts of the account with the same article make one line); the unit price
     * follows the amount.
     */
    public void add(BigDecimal partQuantity, BigDecimal partAmount) {
        quantity = quantity.add(partQuantity);
        amount = amount.add(partAmount);
        unitPrice = quantity.signum() == 0 ? BigDecimal.ZERO
                : amount.divide(quantity, UNIT_PRICE_SCALE, RoundingMode.HALF_UP);
    }

    public void transferred(LocalDateTime at) {
        this.exportStatus = ExportStatus.TRANSFERRED;
        this.exportDate = at;
    }

    /**
     * The ERP's answer: booked, or refused with its text.
     */
    public void answered(boolean booked, String text, LocalDateTime at) {
        this.exportStatus = booked ? ExportStatus.PROCESSED : ExportStatus.ERROR;
        this.errorText = booked ? null : text;
        this.answerDate = at;
    }

    /**
     * A refused line goes to the ERP again with the next file.
     */
    public void resend() {
        if (exportStatus != ExportStatus.ERROR) {
            throw new IllegalStateException("Only refused lines are sent again, line " + id + " is " + exportStatus);
        }
        this.exportStatus = ExportStatus.NEW;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ErpBillingLine other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "ErpBillingLine{" + billingPeriod + ", account='" + billingAccountNo + "', " + lineType + " "
                + articleNo + " " + quantity.stripTrailingZeros().toPlainString() + " x " + unitPrice + " = "
                + amount + ", " + exportStatus + "}";
    }
}
