package io.pillopl.card;

import java.time.LocalDate;

/**
 * Answer of the authorization to the pedestal: the status, the guest's name for the pedestal display and the last
 * day the card is valid (the controller may cache the card until then).
 */
public class AuthorizationResult {

    private ValidationStatus status;

    private String guestName;

    private LocalDate expiryDate;

    public ValidationStatus getStatus() {
        return status;
    }

    public void setStatus(ValidationStatus status) {
        this.status = status;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public boolean isAccepted() {
        return status == ValidationStatus.ACCEPTED;
    }

    @Override
    public String toString() {
        return "AuthorizationResult{" + status + ", guest='" + guestName + "', expiry=" + expiryDate + "}";
    }
}
