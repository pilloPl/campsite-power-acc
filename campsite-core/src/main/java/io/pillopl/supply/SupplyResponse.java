package io.pillopl.supply;

import io.pillopl.card.ValidationStatus;

import java.time.LocalDate;

/**
 * Answer of the supply control unit to the pedestal link: the status for the card, its last valid day, and for a
 * start the transaction id (0 when refused), the current limit, the price text and the greeting for the display.
 */
public class SupplyResponse {

    private ValidationStatus status;

    private LocalDate expiryDate;

    private Integer transactionId;

    private Integer maxCurrentA;

    private String priceInformation;

    private String greeting;

    public ValidationStatus getStatus() {
        return status;
    }

    public void setStatus(ValidationStatus status) {
        this.status = status;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Integer getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Integer transactionId) {
        this.transactionId = transactionId;
    }

    public Integer getMaxCurrentA() {
        return maxCurrentA;
    }

    public void setMaxCurrentA(Integer maxCurrentA) {
        this.maxCurrentA = maxCurrentA;
    }

    public String getPriceInformation() {
        return priceInformation;
    }

    public void setPriceInformation(String priceInformation) {
        this.priceInformation = priceInformation;
    }

    public String getGreeting() {
        return greeting;
    }

    public void setGreeting(String greeting) {
        this.greeting = greeting;
    }

    public boolean isAccepted() {
        return status == ValidationStatus.ACCEPTED;
    }

    @Override
    public String toString() {
        return "SupplyResponse{" + status + ", tx=" + transactionId + ", expiry=" + expiryDate + ", maxCurrent="
                + maxCurrentA + ", price='" + priceInformation + "', greeting='" + greeting + "'}";
    }
}
